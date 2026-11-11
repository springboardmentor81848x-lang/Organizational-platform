import Logo from "./Logo.jsx";

const PEOPLE = [
  { cls: "pcard--a", initials: "PN", name: "Priya Nair", role: "Data Analyst", skill: "SQL", gap: 82, tone: "blue" },
  { cls: "pcard--b", initials: "DK", name: "Daniel Kim", role: "Product Manager", skill: "Roadmapping", gap: 64, tone: "teal" },
  { cls: "pcard--c", initials: "AO", name: "Amara Okafor", role: "Cloud Engineer", skill: "Kubernetes", gap: 38, tone: "amber" },
];

function Globe() {
  const nodes = [
    [210, 96], [286, 138], [318, 214], [270, 288], [186, 318], [112, 276], [96, 188], [140, 118], [212, 206],
  ];
  const links = [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 7], [7, 0], [8, 0], [8, 2], [8, 4], [8, 6], [8, 1], [8, 5]];
  return (
    <svg className="globe" viewBox="0 0 420 420" role="img" aria-label="Network globe connecting employees and skills">
      <defs>
        <radialGradient id="gl-fill" cx="38%" cy="32%" r="75%">
          <stop offset="0" stopColor="#ffffff" />
          <stop offset="0.55" stopColor="#d9e8fb" />
          <stop offset="1" stopColor="#a9c8f2" />
        </radialGradient>
        <clipPath id="gl-clip"><circle cx="210" cy="210" r="150" /></clipPath>
      </defs>
      <ellipse className="globe__shadow" cx="210" cy="378" rx="120" ry="12" />
      <circle cx="210" cy="210" r="150" fill="url(#gl-fill)" />
      <g clipPath="url(#gl-clip)" className="globe__grid">
        <ellipse cx="210" cy="210" rx="150" ry="52" />
        <ellipse cx="210" cy="210" rx="150" ry="104" />
        <ellipse cx="210" cy="210" rx="52" ry="150" />
        <ellipse cx="210" cy="210" rx="104" ry="150" />
        <line x1="60" y1="210" x2="360" y2="210" />
        <line x1="210" y1="60" x2="210" y2="360" />
      </g>
      <circle cx="210" cy="210" r="150" fill="none" stroke="#1f4e92" strokeOpacity=".28" strokeWidth="1.5" />
      <g className="globe__links">
        {links.map(([a, b], i) => (
          <line key={i} x1={nodes[a][0]} y1={nodes[a][1]} x2={nodes[b][0]} y2={nodes[b][1]} />
        ))}
      </g>
      {nodes.map(([x, y], i) => (
        <g key={i}>
          <circle cx={x} cy={y} r={i === 8 ? 11 : 7} fill={i === 8 ? "#2563eb" : "#0b2545"} />
          <circle cx={x} cy={y} r={i === 8 ? 4 : 2.6} fill="#fff" />
        </g>
      ))}
      <ellipse className="globe__orbit" cx="210" cy="210" rx="196" ry="62" transform="rotate(-18 210 210)" />
    </svg>
  );
}

export default function IllustrationPanel() {
  return (
    <aside className="hero" aria-label="About SkillSphere">
      <div className="hero__brand"><Logo /></div>

      <div className="hero__art" aria-hidden="false">
        <Globe />
        {PEOPLE.map((p) => (
          <div key={p.name} className={`pcard ${p.cls}`} aria-hidden="true">
            <span className={`pcard__avatar pcard__avatar--${p.tone}`}>{p.initials}</span>
            <div className="pcard__body">
              <strong>{p.name}</strong>
              <span>{p.role}</span>
              <div className="pcard__skill">
                <em>{p.skill}</em>
                <span className="pcard__meter"><i style={{ width: `${p.gap}%` }} /></span>
                <b>{p.gap}%</b>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="hero__copy">
        <h2>Bridge the Gap. Build Your Future.</h2>
        <p>
          Discover skill gaps, explore learning opportunities, and grow your professional potential with SkillSphere.
        </p>
      </div>
    </aside>
  );
}
