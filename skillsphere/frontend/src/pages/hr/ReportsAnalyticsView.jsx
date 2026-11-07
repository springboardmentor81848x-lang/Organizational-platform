import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  generateHrReport,
  shareHrReportWithLeadership,
  getSharedLeadershipReports,
} from "../../services/hrService.js";
import { getDepartments } from "../../services/platformService.js";

const REPORT_TYPES = [
  { id: "WORKFORCE_READINESS", name: "Workforce Readiness & Skill Intelligence" },
  { id: "SKILL_GAP_ANALYSIS", name: "Organizational Skill Gap Analysis" },
  { id: "LEARNING_PROGRESS", name: "Workforce Learning & Training Progress" },
  { id: "COMPETENCY_AUDIT", name: "Competency Framework Audit" },
];

export default function ReportsAnalyticsView() {
  const [selectedType, setSelectedType] = useState("WORKFORCE_READINESS");
  const [selectedDept, setSelectedDept] = useState("");
  const [departments, setDepartments] = useState([]);
  const [reportResult, setReportResult] = useState(null);
  const [sharedReports, setSharedReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Share Modal
  const [showShareModal, setShowShareModal] = useState(false);
  const [shareRole, setShareRole] = useState("ALL_LEADERSHIP");
  const [shareNotes, setShareNotes] = useState("");
  const [sharing, setSharing] = useState(false);

  const loadInitial = async () => {
    try {
      const [depts, shared] = await Promise.all([
        getDepartments().catch(() => []),
        getSharedLeadershipReports().catch(() => []),
      ]);
      setDepartments(depts);
      setSharedReports(shared);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    loadInitial();
    handleGenerate();
  }, []);

  const handleGenerate = async () => {
    try {
      setLoading(true);
      setError("");
      setSuccess("");
      const res = await generateHrReport({
        reportType: selectedType,
        departmentId: selectedDept ? Number(selectedDept) : null,
      });
      setReportResult(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to generate report.");
    } finally {
      setLoading(false);
    }
  };

  const handleOpenShare = () => {
    if (!reportResult) return;
    setShareRole("ALL_LEADERSHIP");
    setShareNotes("");
    setShowShareModal(true);
  };

  const handleCloseShare = () => {
    setShowShareModal(false);
  };

  const handleConfirmShare = async (e) => {
    e.preventDefault();
    if (!reportResult) return;

    try {
      setSharing(true);
      setError("");
      setSuccess("");

      await shareHrReportWithLeadership({
        reportType: reportResult.reportType,
        title: reportResult.title,
        sharedWithRole: shareRole,
        departmentId: selectedDept ? Number(selectedDept) : null,
        summaryNotes: shareNotes.trim(),
        reportDataJson: JSON.stringify(reportResult.data),
      });

      setSuccess(`Report "${reportResult.title}" shared with ${shareRole}.`);
      setShowShareModal(false);
      const updatedShared = await getSharedLeadershipReports();
      setSharedReports(updatedShared);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to share report.");
    } finally {
      setSharing(false);
    }
  };

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Reports, Analytics & Leadership Briefings</h3>
            <p className="hr-card-subtitle">
              Generate enterprise intelligence reports and distribute approved summaries to Managers and Department Heads
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <button
              className="btn-primary btn-sm"
              onClick={handleOpenShare}
              disabled={!reportResult || loading}
            >
              <Icon name="share" size={14} /> Share with Leadership
            </button>
          </div>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}
          {success && <Alert type="success" message={success} />}

          {/* Configuration Bar */}
          <div className="hr-filter-bar">
            <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
              <label style={{ fontSize: "12px", fontWeight: 600, color: "var(--hr-soft)" }}>
                Select Report Subject:
              </label>
              <select
                className="form-control"
                style={{ minWidth: 260 }}
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
              >
                {REPORT_TYPES.map((rt) => (
                  <option key={rt.id} value={rt.id}>
                    {rt.name}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
              <label style={{ fontSize: "12px", fontWeight: 600, color: "var(--hr-soft)" }}>
                Scope Department:
              </label>
              <select
                className="form-control"
                style={{ minWidth: 180 }}
                value={selectedDept}
                onChange={(e) => setSelectedDept(e.target.value)}
              >
                <option value="">All Departments (Enterprise)</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ display: "flex", alignItems: "flex-end", height: "100%", marginTop: "18px" }}>
              <button className="btn-primary" onClick={handleGenerate} disabled={loading}>
                {loading ? "Compiling Report..." : "Generate Live Report"}
              </button>
            </div>
          </div>

          {/* Report Viewer */}
          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Compiling workforce intelligence metrics...</h4>
            </div>
          ) : reportResult ? (
            <div
              style={{
                marginTop: "20px",
                border: "1px solid var(--hr-line)",
                borderRadius: "8px",
                background: "#ffffff",
                padding: "24px",
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "flex-start",
                  borderBottom: "1px solid var(--hr-line)",
                  paddingBottom: "14px",
                  marginBottom: "18px",
                }}
              >
                <div>
                  <h3 style={{ margin: "0 0 4px", fontSize: "18px", fontWeight: 700 }}>
                    {reportResult.title}
                  </h3>
                  <div style={{ fontSize: "12.5px", color: "var(--hr-soft)" }}>
                    Generated on {new Date(reportResult.generatedAt).toLocaleString()} • Scope:{" "}
                    {selectedDept
                      ? departments.find((d) => d.id === Number(selectedDept))?.name || "Department"
                      : "Enterprise-wide"}
                  </div>
                </div>
                <span className="status-pill success">Verified DB-Backed</span>
              </div>

              {/* Structured Metrics View */}
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "14px", marginBottom: "20px" }}>
                {Object.entries(reportResult.data || {}).map(([key, val]) => {
                  if (typeof val === "object" && val !== null) return null;
                  return (
                    <div
                      key={key}
                      style={{
                        padding: "12px",
                        background: "#f8fafc",
                        borderRadius: "6px",
                        border: "1px solid var(--hr-line)",
                      }}
                    >
                      <div style={{ fontSize: "11px", textTransform: "uppercase", color: "var(--hr-soft)", fontWeight: 700 }}>
                        {key.replace(/([A-Z])/g, " $1")}
                      </div>
                      <div style={{ fontSize: "20px", fontWeight: 800, color: "var(--hr-ink)", marginTop: "4px" }}>
                        {String(val)}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* JSON preview */}
              <details style={{ marginTop: "14px" }}>
                <summary style={{ cursor: "pointer", fontSize: "12.5px", color: "var(--hr-primary)", fontWeight: 600 }}>
                  View Raw Data Payload
                </summary>
                <pre
                  style={{
                    background: "#0f172a",
                    color: "#f8fafc",
                    padding: "14px",
                    borderRadius: "6px",
                    fontSize: "12px",
                    overflowX: "auto",
                    marginTop: "8px",
                  }}
                >
                  {JSON.stringify(reportResult.data, null, 2)}
                </pre>
              </details>
            </div>
          ) : null}

          {/* Shared Reports Archive */}
          <div style={{ marginTop: "36px" }}>
            <h4 style={{ margin: "0 0 12px", fontSize: "15px", fontWeight: 700 }}>
              Leadership Reports Distribution Archive ({sharedReports.length})
            </h4>
            {sharedReports.length === 0 ? (
              <p style={{ fontSize: "13px", color: "var(--hr-soft)" }}>
                No reports shared with leadership yet. Click "Share with Leadership" above to distribute reports to Managers or Department Heads.
              </p>
            ) : (
              <div className="hr-table-wrap">
                <table className="hr-table">
                  <thead>
                    <tr>
                      <th>Report Title</th>
                      <th>Shared With</th>
                      <th>Department</th>
                      <th>Executive Notes</th>
                      <th>Shared By</th>
                      <th>Date Shared</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sharedReports.map((s) => (
                      <tr key={s.id}>
                        <td style={{ fontWeight: 600 }}>{s.title}</td>
                        <td>
                          <span className="status-pill purple">{s.sharedWithRole}</span>
                        </td>
                        <td>{s.departmentName}</td>
                        <td style={{ maxWidth: 320, fontSize: "12.5px", color: "var(--hr-soft)" }}>
                          {s.summaryNotes || "—"}
                        </td>
                        <td>{s.sharedByName}</td>
                        <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                          {new Date(s.createdAt).toLocaleDateString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Share with Leadership Modal */}
      {showShareModal && (
        <div className="hr-modal-overlay" onClick={handleCloseShare}>
          <div className="hr-modal" onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleConfirmShare}>
              <div className="hr-modal-header">
                <h3>Share Report with Leadership</h3>
                <button type="button" className="hr-modal-close" onClick={handleCloseShare}>
                  &times;
                </button>
              </div>

              <div className="hr-modal-body">
                <div style={{ marginBottom: "16px", padding: "10px 14px", background: "var(--hr-primary-tint)", borderRadius: "6px", fontSize: "13px" }}>
                  Sharing: <strong>{reportResult?.title}</strong>
                </div>

                <div className="form-group">
                  <label className="form-label">Target Leadership Audience *</label>
                  <select
                    className="form-control"
                    value={shareRole}
                    onChange={(e) => setShareRole(e.target.value)}
                    required
                  >
                    <option value="ALL_LEADERSHIP">All Leadership (Managers & Department Heads)</option>
                    <option value="MANAGER">Team Leads & Managers</option>
                    <option value="DEPARTMENT_HEAD">Department Heads Only</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Executive Summary & Instructions</label>
                  <textarea
                    className="form-control"
                    placeholder="Provide context, critical action items, or focus areas for managers..."
                    value={shareNotes}
                    onChange={(e) => setShareNotes(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="hr-modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseShare}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={sharing}>
                  {sharing ? "Sharing..." : "Distribute to Leadership"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
