/** Target proficiency (out of 5) expected for each assessed skill. */
export const SKILL_TARGETS = {
  Java: 4,
  SQL: 4,
  "Web Development": 3,
  "Problem Solving": 4,
  "Data Structures": 4,
  "Git and Version Control": 3,
};

export const QUESTIONS = [
  { skill: "Java", question: "Which keyword is used to inherit a class in Java?", options: ["implements", "extends", "inherits", "super"], answer: 1 },
  { skill: "Java", question: "Which collection stores only unique elements?", options: ["ArrayList", "HashSet", "LinkedList", "ArrayDeque"], answer: 1 },
  { skill: "SQL", question: "Which SQL clause filters rows after they have been grouped?", options: ["WHERE", "HAVING", "ORDER BY", "LIMIT"], answer: 1 },
  { skill: "SQL", question: "Which JOIN returns only the rows that match in both tables?", options: ["LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "CROSS JOIN"], answer: 2 },
  { skill: "Web Development", question: "Which language is used to style a web page?", options: ["HTML", "CSS", "SQL", "Java"], answer: 1 },
  { skill: "Web Development", question: "Which HTML element is best suited for a site's main navigation links?", options: ["<nav>", "<div>", "<section>", "<span>"], answer: 0 },
  { skill: "Problem Solving", question: "What is the best first step when debugging a failing feature?", options: ["Rewrite the whole feature", "Ignore the error message", "Reproduce the issue and read the error", "Delete the project"], answer: 2 },
  { skill: "Problem Solving", question: "A page loads slowly. What should you do before optimising?", options: ["Guess the cause and change code", "Measure to find the real bottleneck", "Add caching everywhere", "Buy a bigger server"], answer: 1 },
  { skill: "Data Structures", question: "Which structure offers average O(1) lookup by key?", options: ["Linked list", "Hash map", "Sorted array", "Stack"], answer: 1 },
  { skill: "Data Structures", question: "Which structure follows last-in, first-out (LIFO) order?", options: ["Queue", "Stack", "Heap", "Graph"], answer: 1 },
  { skill: "Git and Version Control", question: "Which command creates a new branch and switches to it?", options: ["git branch -d", "git checkout -b", "git merge", "git stash"], answer: 1 },
  { skill: "Git and Version Control", question: "Which command downloads remote changes and merges them into your branch?", options: ["git fetch", "git pull", "git push", "git clone"], answer: 1 },
];

/** Turns the answers into per-skill results (the payload Gemini receives). */
export function scoreAssessment(answers) {
  const stats = {};
  QUESTIONS.forEach((q, i) => {
    stats[q.skill] ||= { skill: q.skill, correct: 0, total: 0 };
    stats[q.skill].total += 1;
    if (answers[i] === q.answer) stats[q.skill].correct += 1;
  });
  const skillResults = Object.values(stats).map((s) => {
    const level = Math.max(1, Math.round((s.correct / s.total) * 5));
    const required = SKILL_TARGETS[s.skill] ?? 4;
    const gap = Math.max(0, required - level);
    return { ...s, level, required, gap, priority: gap >= 2 ? "High" : gap === 1 ? "Medium" : "On track" };
  });
  const correct = skillResults.reduce((n, s) => n + s.correct, 0);
  return {
    answers,
    correct,
    total: QUESTIONS.length,
    score: Math.round((correct / QUESTIONS.length) * 100),
    skillResults,
    completedAt: new Date().toISOString(),
  };
}
