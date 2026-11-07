package com.skillsphere.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

    private final SkillAssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentSubmissionRepository submissionRepository;
    private final AssessmentResponseRepository responseRepository;
    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SkillGapRepository skillGapRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final RoleSkillRequirementRepository requirementRepository;
    private final EmployeeProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final NotificationService notificationService;
    private final GeminiAssessmentQuestionService geminiQuestionService;
    private final ObjectMapper objectMapper;

    public AssessmentService(SkillAssessmentRepository assessmentRepository,
                             AssessmentQuestionRepository questionRepository,
                             AssessmentSubmissionRepository submissionRepository,
                             AssessmentResponseRepository responseRepository,
                             AssessmentAssignmentRepository assignmentRepository,
                             AssessmentAttemptRepository attemptRepository,
                             SkillGapRepository skillGapRepository,
                             EmployeeSkillRepository employeeSkillRepository,
                             RoleSkillRequirementRepository requirementRepository,
                             EmployeeProfileRepository profileRepository,
                             UserRepository userRepository,
                             SkillRepository skillRepository,
                             TeamMembershipRepository teamMembershipRepository,
                             TeamRepository teamRepository,
                             DepartmentRepository departmentRepository,
                             MentorAssignmentRepository mentorAssignmentRepository,
                             NotificationService notificationService,
                             GeminiAssessmentQuestionService geminiQuestionService,
                             ObjectMapper objectMapper) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.responseRepository = responseRepository;
        this.assignmentRepository = assignmentRepository;
        this.attemptRepository = attemptRepository;
        this.skillGapRepository = skillGapRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.requirementRepository = requirementRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.teamRepository = teamRepository;
        this.departmentRepository = departmentRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.notificationService = notificationService;
        this.geminiQuestionService = geminiQuestionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AssessmentSummaryDto> getAllAssessments() {
        return assessmentRepository.findAll().stream().map(a -> {
            List<AssessmentQuestion> questions = questionRepository.findByAssessmentId(a.getId());
            return new AssessmentSummaryDto(
                    a.getId(),
                    a.getTitle(),
                    a.getDescription(),
                    a.getCategory() != null ? a.getCategory().getId() : null,
                    a.getCategory() != null ? a.getCategory().getName() : "General",
                    a.getTargetRole(),
                    a.getPassScore(),
                    a.getTimeLimitMinutes(),
                    questions.size()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public AssessmentDetailDto getAssessmentById(Long id) {
        SkillAssessment a = assessmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment not found."));

        List<AssessmentQuestion> questions = questionRepository.findByAssessmentId(a.getId());
        List<QuestionDto> questionDtos = questions.stream().map(q -> new QuestionDto(
                q.getId(),
                q.getSkill().getId(),
                q.getSkill().getName(),
                q.getQuestionText(),
                parseOptions(q.getOptionsJson()),
                q.getPoints(),
                q.getDifficulty(),
                q.getSubtopic(),
                q.getQuestionOrder()
        )).toList();

        return new AssessmentDetailDto(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getTargetRole(),
                a.getPassScore(),
                a.getTimeLimitMinutes(),
                questionDtos
        );
    }

    /* ---------------------------------------------------------------- Dynamic Assessment Lifecycle */

    @Transactional
    public synchronized AssessmentAttemptDto startAssessment(String email, StartAssessmentRequest req) {
        User user = getUserByEmail(email);

        // Transactional locking: lock the user's row in PostgreSQL to serialize concurrent start requests
        user = userRepository.findByIdForUpdate(user.getId()).orElse(user);

        Skill skill = resolveSkill(req);
        String targetLevel = normalizeProficiencyLevel(req.proficiencyLevel());

        String currentProficiency = employeeSkillRepository.findByUserIdAndSkillId(user.getId(), skill.getId())
                .map(EmployeeSkill::getProficiencyLevel)
                .orElse("Unaware");

        // 1. RECOVERY CHECK: Return existing in-progress attempt if one already exists
        List<AssessmentAttempt> activeAttempts = attemptRepository.findByUserIdAndSkillIdAndStatus(user.getId(), skill.getId(), "IN_PROGRESS");
        if (!activeAttempts.isEmpty()) {
            AssessmentAttempt existing = activeAttempts.get(0);
            List<AssessmentQuestion> existingQuestions = questionRepository.findByAttemptIdOrderByQuestionOrderAsc(existing.getId());
            if (!existingQuestions.isEmpty()) {
                log.info("Returning existing IN_PROGRESS assessment attempt #{} for user '{}' and skill '{}'",
                        existing.getId(), user.getEmail(), skill.getName());
                return toAssessmentAttemptDto(existing, existingQuestions);
            }
        }

        // 2. Resolve or find matching SkillAssessment
        SkillAssessment assessment = resolveSkillAssessment(req, skill);

        AssessmentAssignment assignment = null;
        if (req.assignmentId() != null) {
            assignment = assignmentRepository.findById(req.assignmentId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment assignment not found."));
            if (!assignment.getUser().getId().equals(user.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Assignment belongs to another employee.");
            }
            if ("COMPLETED".equalsIgnoreCase(assignment.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "This assessment assignment has already been completed.");
            }
        }

        // 3. Collect recent question history to prevent duplicates (last 5 attempts)
        List<AssessmentAttempt> recentAttempts = attemptRepository.findTop5ByUserIdAndSkillIdOrderByStartedAtDesc(user.getId(), skill.getId());
        List<String> previousQuestionTexts = new ArrayList<>();
        for (AssessmentAttempt past : recentAttempts) {
            List<AssessmentQuestion> pastQs = questionRepository.findByAttemptIdOrderByQuestionOrderAsc(past.getId());
            for (AssessmentQuestion pq : pastQs) {
                previousQuestionTexts.add(pq.getQuestionText());
            }
        }

        // 4. Generate questions via Gemini or trigger controlled Fallback
        int requestedCount = 10;
        List<GeminiAssessmentQuestionService.GeneratedQuestion> generated = null;
        String generationSource = "FALLBACK";
        String generationStatus = "SUCCESS";
        String generationError = null;

        if (geminiQuestionService != null && geminiQuestionService.isConfigured()) {
            try {
                generated = geminiQuestionService.generateQuestions(skill, targetLevel, currentProficiency, requestedCount, previousQuestionTexts);
                generationSource = "AI_GENERATED";
                generationStatus = "SUCCESS";
            } catch (Exception e) {
                log.warn("Gemini question generation failed ({}); activating controlled database fallback.", e.getMessage());
                generationSource = "FALLBACK";
                generationStatus = "FALLBACK_TRIGGERED";
                generationError = e.getMessage();
            }
        } else {
            log.info("Gemini not configured; using controlled database fallback question bank for skill '{}'", skill.getName());
            generationSource = "FALLBACK";
            generationStatus = "FALLBACK_TRIGGERED";
        }

        // 5. Create attempt record
        AssessmentAttempt attempt = new AssessmentAttempt(
                user,
                skill,
                assessment,
                assignment,
                targetLevel,
                currentProficiency,
                generationSource
        );
        attempt.setGenerationStatus(generationStatus);
        attempt.setGenerationError(generationError);
        attempt = attemptRepository.save(attempt);

        List<AssessmentQuestion> savedQuestions = new ArrayList<>();

        if ("AI_GENERATED".equals(generationSource) && generated != null && !generated.isEmpty()) {
            int order = 1;
            for (GeminiAssessmentQuestionService.GeneratedQuestion gq : generated) {
                String optionsJson;
                try {
                    optionsJson = objectMapper.writeValueAsString(gq.options());
                } catch (Exception e) {
                    optionsJson = "[\"A\", \"B\", \"C\", \"D\"]";
                }
                AssessmentQuestion aq = new AssessmentQuestion(
                        assessment,
                        skill,
                        attempt,
                        gq.questionText(),
                        optionsJson,
                        gq.correctAnswerIndex(),
                        gq.points(),
                        gq.difficulty(),
                        gq.subtopic(),
                        gq.explanation(),
                        order++
                );
                savedQuestions.add(questionRepository.save(aq));
            }
        } else {
            // Select from database fallback pool
            savedQuestions = selectFallbackQuestions(skill, attempt, assessment, targetLevel, requestedCount, previousQuestionTexts);
        }

        if (savedQuestions.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Insufficient assessment questions available for skill: " + skill.getName());
        }

        attempt.setTotalQuestions(savedQuestions.size());
        attemptRepository.save(attempt);

        return toAssessmentAttemptDto(attempt, savedQuestions);
    }

    private List<AssessmentQuestion> selectFallbackQuestions(Skill skill,
                                                             AssessmentAttempt attempt,
                                                             SkillAssessment assessment,
                                                             String targetLevel,
                                                             int count,
                                                             List<String> previousQuestionTexts) {
        List<AssessmentQuestion> pool = questionRepository.findBySkillIdAndAttemptIdIsNull(skill.getId());
        if (pool.isEmpty()) {
            pool = questionRepository.findAll().stream().filter(q -> q.getAttempt() == null).toList();
        }

        if (pool.isEmpty()) {
            return Collections.emptyList();
        }

        List<AssessmentQuestion> easyList = new ArrayList<>(pool.stream().filter(q -> "EASY".equalsIgnoreCase(q.getDifficulty())).toList());
        List<AssessmentQuestion> medList = new ArrayList<>(pool.stream().filter(q -> "MEDIUM".equalsIgnoreCase(q.getDifficulty())).toList());
        List<AssessmentQuestion> hardList = new ArrayList<>(pool.stream().filter(q -> "HARD".equalsIgnoreCase(q.getDifficulty())).toList());

        int targetEasy = (int) Math.round(count * 0.3);
        int targetHard = (int) Math.round(count * 0.3);
        int targetMed = count - targetEasy - targetHard;

        List<AssessmentQuestion> selectedTemplates = new ArrayList<>();
        selectedTemplates.addAll(pickNonDuplicate(easyList, targetEasy, previousQuestionTexts));
        selectedTemplates.addAll(pickNonDuplicate(medList, targetMed, previousQuestionTexts));
        selectedTemplates.addAll(pickNonDuplicate(hardList, targetHard, previousQuestionTexts));

        if (selectedTemplates.size() < count) {
            for (AssessmentQuestion q : pool) {
                if (!selectedTemplates.contains(q)) {
                    selectedTemplates.add(q);
                    if (selectedTemplates.size() == count) break;
                }
            }
        }

        List<AssessmentQuestion> savedQuestions = new ArrayList<>();
        int order = 1;
        for (AssessmentQuestion template : selectedTemplates) {
            AssessmentQuestion copy = new AssessmentQuestion(
                    assessment,
                    skill,
                    attempt,
                    template.getQuestionText(),
                    template.getOptionsJson(),
                    template.getCorrectAnswerIndex(),
                    template.getPoints(),
                    template.getDifficulty(),
                    template.getSubtopic() != null ? template.getSubtopic() : skill.getName(),
                    template.getExplanation(),
                    order++
            );
            savedQuestions.add(questionRepository.save(copy));
        }

        return savedQuestions;
    }

    private List<AssessmentQuestion> pickNonDuplicate(List<AssessmentQuestion> pool, int needed, List<String> previousQuestionTexts) {
        List<AssessmentQuestion> chosen = new ArrayList<>();
        for (AssessmentQuestion q : pool) {
            String norm = GeminiAssessmentQuestionService.normalizeText(q.getQuestionText());
            if (!GeminiAssessmentQuestionService.isDuplicateOfPrevious(norm, previousQuestionTexts)) {
                chosen.add(q);
                if (chosen.size() == needed) return chosen;
            }
        }
        for (AssessmentQuestion q : pool) {
            if (!chosen.contains(q)) {
                chosen.add(q);
                if (chosen.size() == needed) return chosen;
            }
        }
        return chosen;
    }

    @Transactional(readOnly = true)
    public AssessmentAttemptDto getOngoingAttempt(String email) {
        User user = getUserByEmail(email);
        List<AssessmentAttempt> active = attemptRepository.findByUserIdAndStatus(user.getId(), "IN_PROGRESS");
        if (active.isEmpty()) {
            return null;
        }
        AssessmentAttempt attempt = active.get(0);
        List<AssessmentQuestion> questions = questionRepository.findByAttemptIdOrderByQuestionOrderAsc(attempt.getId());
        return toAssessmentAttemptDto(attempt, questions);
    }

    @Transactional(readOnly = true)
    public AssessmentAttemptDto getAttemptById(String email, Long attemptId) {
        User requester = getUserByEmail(email);
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment attempt not found."));

        // If the user is the attempt owner, they can access it in any status
        if (attempt.getUser().getId().equals(requester.getId())) {
            List<AssessmentQuestion> questions = questionRepository.findByAttemptIdOrderByQuestionOrderAsc(attempt.getId());
            return toAssessmentAttemptDto(attempt, questions);
        }

        // Other users cannot view an attempt that is still IN_PROGRESS
        if ("IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "In-progress assessment attempts are private to the employee taking the assessment.");
        }

        // For COMPLETED attempts, verify scoped permissions based on role
        boolean authorized = false;
        Role role = requester.getRole();

        if (role == Role.ADMIN || role == Role.HR_SPECIALIST) {
            authorized = true;
        } else if (role == Role.MANAGER) {
            authorized = teamMembershipRepository.findByManagerUserId(requester.getId()).stream()
                    .anyMatch(tm -> tm.getUser().getId().equals(attempt.getUser().getId()));
            if (!authorized) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Managers can only view assessments for employees in their managed teams.");
            }
        } else if (role == Role.DEPARTMENT_HEAD) {
            Optional<Department> dept = departmentRepository.findByHeadUserId(requester.getId());
            authorized = dept.isPresent() && teamMembershipRepository.findByDepartmentId(dept.get().getId()).stream()
                    .anyMatch(tm -> tm.getUser().getId().equals(attempt.getUser().getId()));
            if (!authorized) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Department heads can only view assessments for employees in their department.");
            }
        } else if (role == Role.MENTOR) {
            authorized = mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(
                    requester.getId(), attempt.getUser().getId(), AssignmentStatus.ACTIVE);
            if (!authorized) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Mentors can only view assessments for their active assigned mentees.");
            }
        }

        if (!authorized) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You do not have permission to view this assessment attempt.");
        }

        List<AssessmentQuestion> questions = questionRepository.findByAttemptIdOrderByQuestionOrderAsc(attempt.getId());
        return toAssessmentAttemptDto(attempt, questions);
    }

    @Transactional
    public void saveAttemptAnswers(String email, Long attemptId, SaveAnswersRequest req) {
        User user = getUserByEmail(email);
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment attempt not found."));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Attempt belongs to another employee.");
        }
        if (!"IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot save answers for completed or expired attempt.");
        }

        try {
            attempt.setAnswersJson(objectMapper.writeValueAsString(req.answers()));
            attemptRepository.save(attempt);
        } catch (Exception e) {
            log.warn("Failed to serialize attempt answers: {}", e.getMessage());
        }
    }

    @Transactional
    public AssessmentResultDto submitAttempt(String email, Long attemptId, SubmitAssessmentRequest req) {
        User user = getUserByEmail(email);
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment attempt not found."));

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Attempt belongs to another employee.");
        }
        if (!"IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This assessment attempt has already been completed.");
        }

        List<AssessmentQuestion> questions = questionRepository.findByAttemptIdOrderByQuestionOrderAsc(attempt.getId());
        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assessment attempt contains no questions.");
        }

        int totalPoints = 0;
        int earnedPoints = 0;
        int correctCount = 0;
        Map<Long, Integer> answers = req.answers() != null ? req.answers() : Collections.emptyMap();
        List<QuestionResultDto> questionResults = new ArrayList<>();

        for (AssessmentQuestion q : questions) {
            totalPoints += q.getPoints();
            Integer selected = answers.get(q.getId());
            int selectedIdx = selected != null ? selected : -1;
            boolean isCorrect = selectedIdx == q.getCorrectAnswerIndex();

            if (isCorrect) {
                earnedPoints += q.getPoints();
                correctCount++;
            }

            questionResults.add(new QuestionResultDto(
                    q.getId(),
                    q.getQuestionText(),
                    q.getSubtopic(),
                    q.getDifficulty(),
                    parseOptions(q.getOptionsJson()),
                    selectedIdx,
                    q.getCorrectAnswerIndex(),
                    isCorrect,
                    q.getExplanation()
            ));
        }

        int scorePercentage = totalPoints > 0 ? (int) Math.round((earnedPoints * 100.0) / totalPoints) : 0;
        int passScore = attempt.getAssessment() != null ? attempt.getAssessment().getPassScore() : 70;
        boolean passed = scorePercentage >= passScore;

        attempt.setStatus("COMPLETED");
        attempt.setScore(scorePercentage);
        attempt.setPassed(passed);
        attempt.setCorrectAnswers(correctCount);
        attempt.setCompletedAt(Instant.now());
        try {
            attempt.setAnswersJson(objectMapper.writeValueAsString(answers));
        } catch (Exception ignored) {}
        attemptRepository.save(attempt);

        AssessmentSubmission submission = new AssessmentSubmission(
                attempt.getAssessment(),
                user,
                attempt.getAssignment(),
                attempt,
                attempt.getSkill(),
                attempt.getGenerationSource(),
                attempt.getTargetLevel(),
                scorePercentage,
                passed
        );
        AssessmentSubmission savedSubmission = submissionRepository.save(submission);

        for (AssessmentQuestion q : questions) {
            Integer selected = answers.get(q.getId());
            boolean isCorrect = selected != null && selected == q.getCorrectAnswerIndex();
            AssessmentResponse resp = new AssessmentResponse(
                    savedSubmission,
                    q,
                    selected != null ? selected : -1,
                    isCorrect
            );
            responseRepository.save(resp);
        }

        if (attempt.getAssignment() != null) {
            attempt.getAssignment().setStatus("COMPLETED");
            assignmentRepository.save(attempt.getAssignment());
        }

        String proficiency = mapPercentageToProficiency(scorePercentage);
        EmployeeSkill es = employeeSkillRepository.findByUserIdAndSkillId(user.getId(), attempt.getSkill().getId())
                .orElseGet(() -> new EmployeeSkill(user, attempt.getSkill(), proficiency));

        int currentNum = SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1);
        int newNum = SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(proficiency, 1);
        if (newNum >= currentNum) {
            es.setProficiencyLevel(proficiency);
            es.setVerified(true);
            es.setUpdatedAt(Instant.now());
            employeeSkillRepository.save(es);
        }

        List<SkillGapDto> updatedGaps = recalculateSkillGapsForUser(user);

        notificationService.createNotification(
                user,
                "ASSESSMENT_RESULT",
                "Assessment Completed: " + attempt.getSkill().getName() + " (" + attempt.getGenerationSource() + ")",
                "You scored " + scorePercentage + "% (" + (passed ? "Passed" : "Did not pass") + ").",
                "/dashboard/employee"
        );

        return new AssessmentResultDto(
                savedSubmission.getId(),
                attempt.getAssessment() != null ? attempt.getAssessment().getId() : 1L,
                attempt.getSkill().getName() + " Assessment",
                scorePercentage,
                passed,
                questions.size(),
                correctCount,
                savedSubmission.getCompletedAt(),
                updatedGaps,
                attempt.getId(),
                attempt.getGenerationSource(),
                attempt.getTargetLevel(),
                attempt.getSkill().getName(),
                questionResults
        );
    }

    /* ---------------------------------------------------------------- Legacy & Standard Submissions */

    @Transactional
    public AssessmentResultDto submitAssessment(String email, Long assessmentId, SubmitAssessmentRequest req) {
        User user = getUserByEmail(email);
        SkillAssessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment not found."));

        List<AssessmentQuestion> questions = questionRepository.findByAssessmentId(assessment.getId());
        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assessment has no questions.");
        }

        int totalPoints = 0;
        int earnedPoints = 0;
        int correctCount = 0;
        Map<Long, Integer> answers = req.answers() != null ? req.answers() : Collections.emptyMap();
        Map<Skill, Integer> skillEarnedPoints = new HashMap<>();
        Map<Skill, Integer> skillTotalPoints = new HashMap<>();
        List<QuestionResultDto> questionResults = new ArrayList<>();

        for (AssessmentQuestion q : questions) {
            totalPoints += q.getPoints();
            Skill skill = q.getSkill();
            skillTotalPoints.put(skill, skillTotalPoints.getOrDefault(skill, 0) + q.getPoints());

            Integer selected = answers.get(q.getId());
            int selectedIdx = selected != null ? selected : -1;
            boolean isCorrect = selected != null && selected == q.getCorrectAnswerIndex();
            if (isCorrect) {
                earnedPoints += q.getPoints();
                correctCount++;
                skillEarnedPoints.put(skill, skillEarnedPoints.getOrDefault(skill, 0) + q.getPoints());
            }

            questionResults.add(new QuestionResultDto(
                    q.getId(),
                    q.getQuestionText(),
                    q.getSubtopic(),
                    q.getDifficulty(),
                    parseOptions(q.getOptionsJson()),
                    selectedIdx,
                    q.getCorrectAnswerIndex(),
                    isCorrect,
                    q.getExplanation()
            ));
        }

        int scorePercentage = totalPoints > 0 ? (int) Math.round((earnedPoints * 100.0) / totalPoints) : 0;
        boolean passed = scorePercentage >= assessment.getPassScore();

        AssessmentAssignment assignment = null;
        if (req.assignmentId() != null) {
            assignment = assignmentRepository.findById(req.assignmentId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment assignment not found."));
            if (!assignment.getUser().getId().equals(user.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. This assignment belongs to another employee.");
            }
            if ("COMPLETED".equalsIgnoreCase(assignment.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "This assessment assignment has already been completed.");
            }
        }

        AssessmentSubmission submission = new AssessmentSubmission(
                assessment,
                user,
                assignment,
                scorePercentage,
                passed
        );
        AssessmentSubmission savedSubmission = submissionRepository.save(submission);

        for (AssessmentQuestion q : questions) {
            Integer selected = answers.get(q.getId());
            boolean isCorrect = selected != null && selected == q.getCorrectAnswerIndex();
            AssessmentResponse resp = new AssessmentResponse(
                    savedSubmission,
                    q,
                    selected != null ? selected : -1,
                    isCorrect
            );
            responseRepository.save(resp);
        }

        if (assignment != null) {
            assignment.setStatus("COMPLETED");
            assignmentRepository.save(assignment);
        }

        for (Map.Entry<Skill, Integer> entry : skillTotalPoints.entrySet()) {
            Skill skill = entry.getKey();
            int sTotal = entry.getValue();
            int sEarned = skillEarnedPoints.getOrDefault(skill, 0);
            int skillPct = (int) Math.round((sEarned * 100.0) / sTotal);
            String proficiency = mapPercentageToProficiency(skillPct);

            EmployeeSkill es = employeeSkillRepository.findByUserIdAndSkillId(user.getId(), skill.getId())
                    .orElseGet(() -> new EmployeeSkill(user, skill, proficiency));
            int currentNum = SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1);
            int newNum = SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(proficiency, 1);
            if (newNum >= currentNum) {
                es.setProficiencyLevel(proficiency);
                es.setVerified(true);
                es.setUpdatedAt(Instant.now());
                employeeSkillRepository.save(es);
            }
        }

        List<SkillGapDto> updatedGaps = recalculateSkillGapsForUser(user);

        notificationService.createNotification(
                user,
                "ASSESSMENT_RESULT",
                "Assessment Completed: " + assessment.getTitle(),
                "You scored " + scorePercentage + "% (" + (passed ? "Passed" : "Did not pass") + ").",
                "/dashboard/employee"
        );

        return new AssessmentResultDto(
                savedSubmission.getId(),
                assessment.getId(),
                assessment.getTitle(),
                scorePercentage,
                passed,
                questions.size(),
                correctCount,
                savedSubmission.getCompletedAt(),
                updatedGaps,
                null,
                "FALLBACK",
                "Intermediate",
                assessment.getTitle(),
                questionResults
        );
    }

    @Transactional
    public List<SkillGapDto> recalculateSkillGapsForUser(User user) {
        String targetRole = null;
        if (profileRepository != null) {
            Optional<EmployeeProfile> profile = profileRepository.findByUserId(user.getId());
            if (profile.isPresent() && profile.get().getJobRole() != null && !profile.get().getJobRole().isBlank()) {
                targetRole = profile.get().getJobRole().trim();
            }
        }
        if (targetRole == null && user.getRequestedRole() != null) {
            targetRole = user.getRequestedRole().name();
        }
        if (targetRole == null) {
            targetRole = "Software Developer";
        }

        List<RoleSkillRequirement> requirements = requirementRepository.findByJobRole(targetRole);
        if (requirements.isEmpty()) {
            requirements = requirementRepository.findByJobRole("Software Developer");
        }
        if (requirements.isEmpty()) {
            requirements = requirementRepository.findAll();
        }

        List<EmployeeSkill> employeeSkills = employeeSkillRepository.findByUserId(user.getId());
        Map<Long, EmployeeSkill> skillMap = new HashMap<>();
        for (EmployeeSkill es : employeeSkills) {
            skillMap.put(es.getSkill().getId(), es);
        }

        List<SkillGapDto> result = new ArrayList<>();
        for (RoleSkillRequirement req : requirements) {
            Skill skill = req.getSkill();
            EmployeeSkill es = skillMap.get(skill.getId());
            String currentProficiency = es != null ? es.getProficiencyLevel() : "Unaware";
            int currentLevel = SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(currentProficiency, 1);
            int requiredLevel = SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(req.getRequiredProficiency(), 3);
            int gapSize = Math.max(0, requiredLevel - currentLevel);

            String priority;
            if (gapSize <= 0) {
                priority = "ON_TRACK";
            } else if (gapSize == 1) {
                priority = "MEDIUM";
            } else {
                priority = "HIGH";
            }

            SkillGap gap = skillGapRepository.findByUserIdAndSkillId(user.getId(), skill.getId())
                    .orElseGet(() -> new SkillGap(user, skill, currentLevel, requiredLevel, gapSize, priority));

            gap.setCurrentLevel(currentLevel);
            gap.setRequiredLevel(requiredLevel);
            gap.setGapSize(gapSize);
            gap.setPriority(priority);
            gap.setCalculatedAt(Instant.now());
            SkillGap savedGap = skillGapRepository.save(gap);

            result.add(new SkillGapDto(
                    skill.getId(),
                    skill.getName(),
                    skill.getCategory().getName(),
                    currentLevel,
                    currentProficiency,
                    requiredLevel,
                    req.getRequiredProficiency(),
                    gapSize,
                    priority,
                    savedGap.getCalculatedAt()
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<SkillGapDto> getUserGaps(String email) {
        User user = getUserByEmail(email);
        List<SkillGap> gaps = skillGapRepository.findByUserId(user.getId());
        if (gaps.isEmpty()) {
            return recalculateSkillGapsForUser(user);
        }
        return gaps.stream().map(this::toSkillGapDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillGapDto> getUserGapsByUserId(String requesterEmail, Long userId) {
        User requester = getUserByEmail(requesterEmail);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target employee not found."));

        if (!requester.getId().equals(target.getId())) {
            Role role = requester.getRole();
            if (role == Role.MANAGER) {
                boolean inTeam = teamMembershipRepository.findByManagerUserId(requester.getId()).stream()
                        .anyMatch(tm -> tm.getUser().getId().equals(target.getId()));
                if (!inTeam) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Managers can only view skill gaps for employees in their managed teams.");
                }
            } else if (role == Role.MENTOR) {
                boolean isMentor = mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(
                        requester.getId(), target.getId(), AssignmentStatus.ACTIVE);
                if (!isMentor) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Mentors can only view skill gaps for their active assigned mentees.");
                }
            } else if (role == Role.DEPARTMENT_HEAD) {
                Optional<Department> dept = departmentRepository.findByHeadUserId(requester.getId());
                boolean inDept = dept.isPresent() && teamMembershipRepository.findByDepartmentId(dept.get().getId()).stream()
                        .anyMatch(tm -> tm.getUser().getId().equals(target.getId()));
                if (!inDept) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Department heads can only view skill gaps for employees in their department.");
                }
            } else if (role != Role.ADMIN && role != Role.HR_SPECIALIST) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You do not have permission to view this employee's skill gaps.");
            }
        }

        List<SkillGap> gaps = skillGapRepository.findByUserId(userId);
        return gaps.stream().map(this::toSkillGapDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillGapDto> getUserGapsByUserId(Long userId) {
        List<SkillGap> gaps = skillGapRepository.findByUserId(userId);
        return gaps.stream().map(this::toSkillGapDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillGapDto> getTeamGaps(String requesterEmail, Long teamId) {
        User requester = getUserByEmail(requesterEmail);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."));

        Role role = requester.getRole();
        if (role == Role.MANAGER) {
            if (team.getManager() == null || !team.getManager().getId().equals(requester.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Managers can only view skill gaps for their own teams.");
            }
        } else if (role == Role.DEPARTMENT_HEAD) {
            Optional<Department> dept = departmentRepository.findByHeadUserId(requester.getId());
            if (dept.isEmpty() || team.getDepartment() == null || !team.getDepartment().getId().equals(dept.get().getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Department heads can only view skill gaps for teams in their department.");
            }
        } else if (role != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You do not have permission to view this team's skill gaps.");
        }

        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(teamId);
        List<Long> userIds = memberships.stream().map(m -> m.getUser().getId()).toList();
        if (userIds.isEmpty()) return Collections.emptyList();
        return skillGapRepository.findByUserIds(userIds).stream().map(this::toSkillGapDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillGapDto> getTeamGaps(Long teamId) {
        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(teamId);
        List<Long> userIds = memberships.stream().map(m -> m.getUser().getId()).toList();
        if (userIds.isEmpty()) return Collections.emptyList();
        return skillGapRepository.findByUserIds(userIds).stream().map(this::toSkillGapDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillGapDto> getAllOrgGaps() {
        return skillGapRepository.findAll().stream().map(this::toSkillGapDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AssessmentAssignmentDto> getUserAssignments(String email) {
        User user = getUserByEmail(email);
        return assignmentRepository.findByUserId(user.getId()).stream().map(a -> new AssessmentAssignmentDto(
                a.getId(),
                a.getAssessment().getId(),
                a.getAssessment().getTitle(),
                a.getAssessment().getPassScore(),
                a.getAssessment().getTimeLimitMinutes(),
                a.getUser().getId(),
                a.getUser().getFullName(),
                a.getAssignedBy().getFullName(),
                a.getDueDate(),
                a.getStatus(),
                a.getCreatedAt()
        )).toList();
    }

    @Transactional
    public AssessmentAssignmentDto assignAssessment(String assignerEmail, AssignAssessmentRequest req) {
        User assigner = getUserByEmail(assignerEmail);
        User target = userRepository.findById(req.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target employee not found."));
        SkillAssessment assessment = assessmentRepository.findById(req.assessmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment not found."));

        // Scope check: MANAGER can only assign assessments to employees within their managed teams
        if (assigner.getRole() == Role.MANAGER) {
            boolean inTeam = teamMembershipRepository.findByManagerUserId(assigner.getId()).stream()
                    .anyMatch(tm -> tm.getUser().getId().equals(target.getId()));
            if (!inTeam) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Managers can only assign assessments to employees in their managed teams.");
            }
        }

        AssessmentAssignment assignment = new AssessmentAssignment(
                assessment,
                target,
                assigner,
                req.dueDate()
        );
        AssessmentAssignment saved = assignmentRepository.save(assignment);

        notificationService.createNotification(
                target,
                "ASSESSMENT_ASSIGNED",
                "New Assessment Assigned: " + assessment.getTitle(),
                "Assigned by " + assigner.getFullName() + ". Due by: " + (req.dueDate() != null ? req.dueDate() : "No deadline"),
                "/dashboard/employee"
        );

        return new AssessmentAssignmentDto(
                saved.getId(),
                assessment.getId(),
                assessment.getTitle(),
                assessment.getPassScore(),
                assessment.getTimeLimitMinutes(),
                target.getId(),
                target.getFullName(),
                assigner.getFullName(),
                saved.getDueDate(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AssessmentResultDto> getUserSubmissions(String email) {
        User user = getUserByEmail(email);
        return submissionRepository.findByUserIdOrderByCompletedAtDesc(user.getId()).stream().map(s -> {
            List<AssessmentResponse> responses = responseRepository.findBySubmissionId(s.getId());
            int total = responses.size();
            int correct = (int) responses.stream().filter(AssessmentResponse::isCorrect).count();
            String skillTitle = s.getSkill() != null ? s.getSkill().getName() : (s.getAssessment() != null ? s.getAssessment().getTitle() : "Assessment");

            return new AssessmentResultDto(
                    s.getId(),
                    s.getAssessment() != null ? s.getAssessment().getId() : null,
                    skillTitle,
                    s.getScore(),
                    s.isPassed(),
                    total,
                    correct,
                    s.getCompletedAt(),
                    Collections.emptyList(),
                    s.getAttempt() != null ? s.getAttempt().getId() : null,
                    s.getGenerationSource() != null ? s.getGenerationSource() : "FALLBACK",
                    s.getTargetLevel() != null ? s.getTargetLevel() : "Intermediate",
                    skillTitle,
                    Collections.emptyList()
            );
        }).toList();
    }

    /* ---------------------------------------------------------------- Helpers */

    public String normalizeProficiencyLevel(String level) {
        if (level == null || level.isBlank()) return "Intermediate";
        for (String canonical : SkillService.PROFICIENCY_TO_NUMERIC.keySet()) {
            if (canonical.equalsIgnoreCase(level.trim())) {
                return canonical;
            }
        }
        return "Intermediate";
    }

    private AssessmentAttemptDto toAssessmentAttemptDto(AssessmentAttempt attempt, List<AssessmentQuestion> questions) {
        Map<Long, Integer> savedAnswers = new HashMap<>();
        if (attempt.getAnswersJson() != null && !attempt.getAnswersJson().isBlank()) {
            try {
                savedAnswers = objectMapper.readValue(attempt.getAnswersJson(), new TypeReference<Map<Long, Integer>>() {});
            } catch (Exception ignored) {}
        }

        // Sanitized QuestionDto - NO correct answers and NO explanations sent to client
        List<QuestionDto> questionDtos = questions.stream().map(q -> new QuestionDto(
                q.getId(),
                q.getSkill().getId(),
                q.getSkill().getName(),
                q.getQuestionText(),
                parseOptions(q.getOptionsJson()),
                q.getPoints(),
                q.getDifficulty(),
                q.getSubtopic(),
                q.getQuestionOrder()
        )).toList();

        return new AssessmentAttemptDto(
                attempt.getId(),
                attempt.getSkill().getId(),
                attempt.getSkill().getName(),
                attempt.getSkill().getCategory() != null ? attempt.getSkill().getCategory().getName() : "General",
                attempt.getTargetLevel(),
                attempt.getCurrentLevel(),
                attempt.getStatus(),
                attempt.getGenerationSource(),
                questions.size(),
                attempt.getAssessment() != null ? attempt.getAssessment().getTimeLimitMinutes() : 25,
                attempt.getAssessment() != null ? attempt.getAssessment().getPassScore() : 70,
                attempt.getStartedAt(),
                savedAnswers,
                questionDtos
        );
    }

    private Skill resolveSkill(StartAssessmentRequest req) {
        if (req.skillId() != null) {
            return skillRepository.findById(req.skillId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found with ID: " + req.skillId()));
        }
        if (req.assessmentId() != null) {
            SkillAssessment sa = assessmentRepository.findById(req.assessmentId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assessment not found with ID: " + req.assessmentId()));
            if (sa.getSkill() != null) {
                return sa.getSkill();
            }
            List<AssessmentQuestion> qs = questionRepository.findByAssessmentId(sa.getId());
            if (!qs.isEmpty() && qs.get(0).getSkill() != null) {
                return qs.get(0).getSkill();
            }
        }
        return skillRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active skills configured."));
    }

    private SkillAssessment resolveSkillAssessment(StartAssessmentRequest req, Skill skill) {
        if (req.assessmentId() != null) {
            return assessmentRepository.findById(req.assessmentId()).orElse(null);
        }
        List<SkillAssessment> all = assessmentRepository.findAll();
        for (SkillAssessment a : all) {
            if (a.getSkill() != null && a.getSkill().getId().equals(skill.getId())) {
                return a;
            }
        }
        return !all.isEmpty() ? all.get(0) : null;
    }

    private List<String> parseOptions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of("Option A", "Option B", "Option C", "Option D");
        }
    }

    public String mapPercentageToProficiency(int pct) {
        if (pct <= 25) return "Unaware";
        if (pct <= 50) return "Beginner";
        if (pct <= 70) return "Intermediate";
        if (pct <= 85) return "Advanced";
        return "Expert";
    }

    private SkillGapDto toSkillGapDto(SkillGap g) {
        return new SkillGapDto(
                g.getSkill().getId(),
                g.getSkill().getName(),
                g.getSkill().getCategory().getName(),
                g.getCurrentLevel(),
                SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getCurrentLevel(), "Unaware"),
                g.getRequiredLevel(),
                SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getRequiredLevel(), "Intermediate"),
                g.getGapSize(),
                g.getPriority(),
                g.getCalculatedAt()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
