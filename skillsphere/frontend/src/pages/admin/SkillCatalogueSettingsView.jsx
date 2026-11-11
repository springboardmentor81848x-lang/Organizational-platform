import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getAdminSkills,
  getAdminSkillCategories,
  createAdminSkill,
  getAdminRoleRequirements,
} from "../../services/adminService.js";

export default function SkillCatalogueSettingsView() {
  const [skills, setSkills] = useState([]);
  const [categories, setCategories] = useState([]);
  const [roleRequirements, setRoleRequirements] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Create Skill Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [skillName, setSkillName] = useState("");
  const [skillCatId, setSkillCatId] = useState("");
  const [skillDesc, setSkillDesc] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [sList, cList, rList] = await Promise.all([
        getAdminSkills(),
        getAdminSkillCategories(),
        getAdminRoleRequirements(),
      ]);
      setSkills(sList || []);
      setCategories(cList || []);
      setRoleRequirements(rList || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load skills.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateSkill = async (e) => {
    e.preventDefault();
    if (!skillName || !skillCatId) {
      setError("Skill name and category are required.");
      return;
    }

    try {
      setError("");
      await createAdminSkill({
        name: skillName.trim(),
        categoryId: Number(skillCatId),
        description: skillDesc.trim(),
      });

      setSuccess(`Skill "${skillName}" created successfully.`);
      setShowCreateModal(false);
      setSkillName("");
      setSkillCatId("");
      setSkillDesc("");
      loadData();
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to create skill.");
    }
  };

  const filteredSkills = skills.filter((s) => {
    if (selectedCategory && s.categoryId !== Number(selectedCategory)) return false;
    if (search) {
      const q = search.toLowerCase();
      return (
        s.name.toLowerCase().contains(q) ||
        (s.description && s.description.toLowerCase().contains(q)) ||
        (s.categoryName && s.categoryName.toLowerCase().contains(q))
      );
    }
    return true;
  });

  return (
    <div>
      <div className="adm-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="adm-panel-title" style={{ margin: 0 }}>
              <Icon name="book" size={18} />
              Platform Competency Catalogue & Skill Registry
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--adm-soft)" }}>
              Centralized definition of verified technical and professional competencies across all departments.
            </p>
          </div>

          <button className="adm-btn adm-btn-primary" onClick={() => setShowCreateModal(true)}>
            <Icon name="plus" size={14} />
            Add Competency
          </button>
        </div>

        {/* Filter bar */}
        <div style={{ display: "flex", gap: "10px", marginTop: "16px", flexWrap: "wrap", alignItems: "center" }}>
          <input
            type="text"
            className="adm-input"
            style={{ flex: 1, minWidth: "220px" }}
            placeholder="Search competencies or descriptions..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <select
            className="adm-select"
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
          >
            <option value="">All Categories ({categories.length})</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}
      {success && <Alert type="success" message={success} />}

      {/* Skills Table */}
      <div className="adm-panel">
        {loading ? (
          <div style={{ textAlign: "center", padding: "60px 20px" }}>
            <Icon name="refresh" className="spin" size={32} />
            <h4 style={{ marginTop: "12px", color: "var(--adm-soft)" }}>Loading Competency Catalogue...</h4>
          </div>
        ) : (
          <div className="adm-table-container">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Competency Name</th>
                  <th>Category</th>
                  <th>Description</th>
                  <th>Role Benchmark Count</th>
                </tr>
              </thead>
              <tbody>
                {filteredSkills.length > 0 ? (
                  filteredSkills.map((s) => {
                    const reqCount = roleRequirements.filter((r) => r.skillId === s.id).length;
                    return (
                      <tr key={s.id}>
                        <td>
                          <strong>{s.name}</strong>
                        </td>
                        <td>
                          <span className="adm-badge">{s.categoryName || "General"}</span>
                        </td>
                        <td style={{ fontSize: "12.5px", color: "var(--adm-soft)", maxWidth: "420px" }}>
                          {s.description || "—"}
                        </td>
                        <td>
                          <span className="adm-status-badge low">{reqCount} Role Requirements</span>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={4} style={{ textAlign: "center", color: "var(--adm-faint)", padding: "20px" }}>
                      No competencies matching the criteria.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Skill Modal */}
      {showCreateModal && (
        <div className="adm-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="adm-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px" }}>Create Skill Definition</h3>
              <button style={{ background: "none", border: "none", cursor: "pointer" }} onClick={() => setShowCreateModal(false)}>
                <Icon name="x" size={20} />
              </button>
            </div>
            <form onSubmit={handleCreateSkill}>
              <div style={{ marginBottom: "12px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Competency Name *
                </label>
                <input
                  type="text"
                  className="adm-input"
                  style={{ width: "100%" }}
                  placeholder="e.g. Kubernetes Cluster Administration"
                  value={skillName}
                  onChange={(e) => setSkillName(e.target.value)}
                  required
                />
              </div>

              <div style={{ marginBottom: "12px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Domain Category *
                </label>
                <select
                  className="adm-select"
                  style={{ width: "100%" }}
                  value={skillCatId}
                  onChange={(e) => setSkillCatId(e.target.value)}
                  required
                >
                  <option value="">Select Category...</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ marginBottom: "20px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Description & Evaluation Standard
                </label>
                <textarea
                  className="adm-input"
                  rows={4}
                  style={{ width: "100%", fontFamily: "inherit" }}
                  placeholder="Define the practical proficiency standard..."
                  value={skillDesc}
                  onChange={(e) => setSkillDesc(e.target.value)}
                />
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
                <button type="button" className="adm-btn adm-btn-secondary" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="adm-btn adm-btn-primary">
                  Save Definition
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
