import { useState } from "react";
import Icon from "../../components/Icons.jsx";

export default function SkillInventoryView({ inventory = [], onNavigateToHeatmap }) {
  const [search, setSearch] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("ALL");

  const categories = ["ALL", ...new Set(inventory.map((i) => i.categoryName).filter(Boolean))];

  const filteredInventory = inventory.filter((item) => {
    const matchesCategory = selectedCategory === "ALL" || item.categoryName === selectedCategory;
    const matchesSearch = !search.trim() || item.skillName.toLowerCase().includes(search.trim().toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const metCount = inventory.filter((i) => i.status === "MET" || i.status === "EXCEEDED").length;
  const gapCount = inventory.filter((i) => i.status === "GAP").length;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header">
        <div>
          <span className="eyebrow">Competency Repository</span>
          <h1>Team Skill Inventory</h1>
          <p>
            Aggregated competency matrix across all team members. Compare actual team proficiency
            against departmental benchmarks and identify skill coverage shortages.
          </p>
        </div>
        <button className="btn btn--ghost" onClick={onNavigateToHeatmap}>
          <Icon name="chart" size={16} /> Open Skill Heatmap
        </button>
      </div>

      {/* KPI Cards */}
      <div className="stat-grid" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))" }}>
        <div className="stat-card">
          <span className="stat-label">Total Skills Tracked</span>
          <strong>{inventory.length}</strong>
          <small>{categories.length - 1} Categories</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Benchmarks Met / Exceeded</span>
          <strong style={{ color: "#15803d" }}>{metCount}</strong>
          <small>{inventory.length > 0 ? Math.round((metCount / inventory.length) * 100) : 0}% of Skills</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Skill Gaps Identified</span>
          <strong style={{ color: gapCount > 0 ? "#dc2626" : "inherit" }}>{gapCount}</strong>
          <small>Action required</small>
        </div>
      </div>

      {/* Filters */}
      <div className="filter-toolbar">
        <div className="filter-group">
          <div style={{ position: "relative" }}>
            <input
              type="text"
              className="filter-input"
              style={{ paddingLeft: "36px", minWidth: "260px" }}
              placeholder="Search skills by name..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <span style={{ position: "absolute", left: "12px", top: "10px", color: "#94a3b8" }}>
              <Icon name="search" size={16} />
            </span>
          </div>

          <select
            className="filter-select"
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
          >
            {categories.map((c) => (
              <option key={c} value={c}>
                {c === "ALL" ? "All Skill Categories" : c}
              </option>
            ))}
          </select>
        </div>

        <div style={{ fontSize: "0.85rem", color: "#64748b", fontWeight: 600 }}>
          Showing {filteredInventory.length} of {inventory.length} Skills
        </div>
      </div>

      {/* Inventory Table */}
      <div className="panel" style={{ padding: 0 }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Skill Name</th>
                <th>Category</th>
                <th>Employees Assessed</th>
                <th>Team Average</th>
                <th>Required Benchmark</th>
                <th>Level Distribution (1 to 5)</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {filteredInventory.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                    No skills found matching your filters.
                  </td>
                </tr>
              ) : (
                filteredInventory.map((item) => (
                  <tr key={item.skillId}>
                    <td>
                      <strong style={{ display: "block" }}>{item.skillName}</strong>
                    </td>
                    <td>
                      <span className="badge" style={{ background: "#f1f5f9", color: "#475569" }}>
                        {item.categoryName}
                      </span>
                    </td>
                    <td>
                      <strong>{item.employeeCount}</strong>
                    </td>
                    <td>
                      <div>
                        <strong style={{ fontSize: "0.95rem" }}>{item.averageProficiencyLevel}</strong> / 5.0
                        <small className="muted">{item.averageProficiencyName}</small>
                      </div>
                    </td>
                    <td>
                      <div>
                        <strong style={{ fontSize: "0.95rem" }}>{item.requiredProficiencyLevel}.0</strong> / 5.0
                        <small className="muted">{item.requiredProficiencyName}</small>
                      </div>
                    </td>
                    <td>
                      <div style={{ display: "flex", gap: "4px", fontSize: "0.74rem", fontWeight: 700 }}>
                        <span title="Unaware" style={{ padding: "2px 6px", borderRadius: "4px", background: "#fee2e2", color: "#991b1b" }}>
                          U: {item.unawareCount}
                        </span>
                        <span title="Beginner" style={{ padding: "2px 6px", borderRadius: "4px", background: "#ffedd5", color: "#9a3412" }}>
                          B: {item.beginnerCount}
                        </span>
                        <span title="Intermediate" style={{ padding: "2px 6px", borderRadius: "4px", background: "#fef3c7", color: "#92400e" }}>
                          I: {item.intermediateCount}
                        </span>
                        <span title="Advanced" style={{ padding: "2px 6px", borderRadius: "4px", background: "#dcfce7", color: "#166534" }}>
                          A: {item.advancedCount}
                        </span>
                        <span title="Expert" style={{ padding: "2px 6px", borderRadius: "4px", background: "#dbeafe", color: "#1e40af" }}>
                          E: {item.expertCount}
                        </span>
                      </div>
                    </td>
                    <td>
                      <span
                        className="badge"
                        style={{
                          background:
                            item.status === "EXCEEDED" ? "#dbeafe" :
                            item.status === "MET" ? "#dcfce7" : "#fee2e2",
                          color:
                            item.status === "EXCEEDED" ? "#1e40af" :
                            item.status === "MET" ? "#166534" : "#991b1b",
                        }}
                      >
                        {item.status}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
