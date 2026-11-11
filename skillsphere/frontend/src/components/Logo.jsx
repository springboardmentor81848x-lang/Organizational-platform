export default function Logo({ light = false }) {
  return (
    <div className={`logo${light ? " logo--light" : ""}`} aria-label="SkillSphere">
      <svg width="34" height="34" viewBox="0 0 32 32" aria-hidden="true">
        <rect width="32" height="32" rx="9" fill="#0b2545" />
        <circle cx="16" cy="16" r="8" fill="none" stroke="#6ea8ff" strokeWidth="2" />
        <ellipse cx="16" cy="16" rx="3.4" ry="8" fill="none" stroke="#6ea8ff" strokeWidth="1.6" />
        <circle cx="23" cy="10" r="2.6" fill="#fff" />
      </svg>
      <span>SkillSphere</span>
    </div>
  );
}
