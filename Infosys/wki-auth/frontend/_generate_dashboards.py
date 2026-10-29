"""Generates one dashboard page per role from _dashboard.template.html.
Edit the tiles below, then run:  python _generate_dashboards.py
"""
from pathlib import Path

here = Path(__file__).parent
template = (here / "_dashboard.template.html").read_text(encoding="utf-8")

ROLES = {
    "ADMIN": ("admin-dashboard.html", "Admin Dashboard", "Super Admin", [
        ("Users & Roles", "Manage accounts and assign roles."),
        ("Departments", "Maintain departments and designations."),
        ("Platform Settings", "Configure the platform."),
    ]),
    "HR_MANAGER": ("hr-dashboard.html", "HR Dashboard", "HR Manager", [
        ("Workforce Overview", "Headcount, departments and skills at a glance."),
        ("Skill Gap Reports", "Identify gaps across the organisation."),
        ("Competency Frameworks", "Define required skills per role."),
    ]),
    "MANAGER": ("manager-dashboard.html", "Team Manager Dashboard", "Team Manager", [
        ("My Team", "See your team members and their skills."),
        ("Team Skill Gaps", "Spot where your team needs training."),
        ("Approvals", "Review training requests."),
    ]),
    "EMPLOYEE": ("employee-dashboard.html", "Employee Dashboard", "Employee", [
        ("My Skills", "View and update your proficiency levels."),
        ("My Learning", "Track enrolled trainings and progress."),
        ("Recommendations", "Trainings suggested for your gaps."),
    ]),
    "TRAINER_MENTOR": ("trainer-dashboard.html", "Trainer / Mentor Dashboard", "Trainer / Mentor", [
        ("My Mentees", "Mentorships you are part of."),
        ("Sessions", "Schedule and run knowledge-sharing sessions."),
        ("Feedback", "Review mentee progress."),
    ]),
    "TRAINING_COORDINATOR": ("coordinator-dashboard.html", "Learning Coordinator Dashboard", "Learning Coordinator", [
        ("Training Programs", "Create and manage programs."),
        ("Learning Paths", "Sequence trainings into paths."),
        ("Enrollments", "Track who is enrolled and completed."),
    ]),
}

for role, (filename, title, label, tiles) in ROLES.items():
    # Pages marked with "custom-page" (e.g. the full employee dashboard) are hand-built: never overwrite them.
    target = here / filename
    if target.exists() and "custom-page" in target.read_text(encoding="utf-8"):
        print("skipped", filename, "(custom page)")
        continue
    tiles_html = "\n".join(
        f'      <div class="tile"><h3>{h}</h3><p>{p}</p></div>' for h, p in tiles
    )
    html = (template.replace("__TITLE__", title)
                    .replace("__ROLE__", role)
                    .replace("__ROLE_LABEL__", label)
                    .replace("__TILES__", tiles_html))
    (here / filename).write_text(html, encoding="utf-8")
    print("wrote", filename)
