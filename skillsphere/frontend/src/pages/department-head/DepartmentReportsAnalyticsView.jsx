import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getDepartmentReport,
  exportDepartmentReportCsv,
  getSharedLeadershipReports,
} from "../../services/departmentHeadService.js";

const REPORT_OPTIONS = [
  { value: "EXECUTIVE_SUMMARY", label: "Executive Summary & KPI Dashboard" },
  { value: "SKILL_INVENTORY", label: "Department Skill & Competency Inventory" },
  { value: "GAP_ANALYSIS", label: "Detailed Departmental Gap Analysis" },
  { value: "COMPETENCY_COMPLIANCE", label: "Role Requirement & Compliance Matrix" },
  { value: "WORKFORCE_READINESS", label: "Workforce Readiness & Benchmarks" },
  { value: "ASSESSMENT_COVERAGE", label: "Assessment Completion & Coverage Status" },
  { value: "TRAINING_IMPACT", label: "Training Programmes & Enrollment Impact" },
  { value: "MENTORSHIP_EFFECTIVENESS", label: "Mentorship Outcomes & Pairing Metrics" },
  { value: "TEAM_CAPABILITY_COMPARISON", label: "Team Comparative Capability Benchmark" },
  { value: "CRITICAL_RISK_REGISTER", label: "Critical Governance & Skill Risk Register" },
  { value: "PROMOTION_SUCCESSION_READINESS", label: "Succession & Senior Role Readiness" },
  { value: "LEARNING_VELOCITY", label: "Learning Velocity & Course Trajectory" },
];

export default function DepartmentReportsAnalyticsView() {
  const [reportType, setReportType] = useState("EXECUTIVE_SUMMARY");
  const [reportData, setReportData] = useState(null);
  const [sharedReports, setSharedReports] = useState([]);
  const [activeTab, setActiveTab] = useState("generate"); // 'generate' | 'shared'
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadReport = async (type = reportType) => {
    try {
      setLoading(true);
      setError("");
      const res = await getDepartmentReport({ reportType: type });
      setReportData(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to generate report.");
    } finally {
      setLoading(false);
    }
  };

  const loadSharedReports = async () => {
    try {
      const res = await getSharedLeadershipReports();
      setSharedReports(res || []);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    loadReport("EXECUTIVE_SUMMARY");
    loadSharedReports();
  }, []);

  const handleExportCsv = async () => {
    try {
      setDownloading(true);
      const res = await exportDepartmentReportCsv({ reportType });
      const blob = new Blob([res.data], { type: "text/csv;charset=utf-8;" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `dept_report_${reportType.toLowerCase()}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      setSuccess("CSV report exported successfully.");
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError("Failed to download CSV: " + (err?.response?.data?.message || err.message));
    } finally {
      setDownloading(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div>
      {/* Controls Bar */}
      <div className="dh-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div style={{ display: "flex", gap: "8px" }}>
            <button
              className={`dh-btn ${activeTab === "generate" ? "dh-btn-primary" : "dh-btn-secondary"}`}
              onClick={() => setActiveTab("generate")}
            >
              <Icon name="file" size={16} />
              Strategic Reports Engine
            </button>
            <button
              className={`dh-btn ${activeTab === "shared" ? "dh-btn-primary" : "dh-btn-secondary"}`}
              onClick={() => setActiveTab("shared")}
            >
              <Icon name="users" size={16} />
              Leadership Shared Archive ({sharedReports.length})
            </button>
          </div>

          {activeTab === "generate" && (
            <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
              <select
                className="dh-select"
                value={reportType}
                onChange={(e) => {
                  setReportType(e.target.value);
                  loadReport(e.target.value);
                }}
              >
                {REPORT_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>

              <button className="dh-btn dh-btn-secondary" onClick={() => loadReport(reportType)} disabled={loading}>
                <Icon name="refresh" size={14} className={loading ? "spin" : ""} />
                Regenerate
              </button>

              <button className="dh-btn dh-btn-secondary" onClick={handleExportCsv} disabled={downloading}>
                <Icon name="file" size={14} />
                {downloading ? "Exporting..." : "Export CSV"}
              </button>

              <button className="dh-btn dh-btn-secondary" onClick={handlePrint}>
                <Icon name="print" size={14} />
                Print
              </button>
            </div>
          )}
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}
      {success && <Alert type="success" message={success} />}

      {activeTab === "generate" ? (
        loading ? (
          <div style={{ textAlign: "center", padding: "60px 20px" }}>
            <Icon name="refresh" className="spin" size={32} />
            <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Compiling Strategic Dataset...</h4>
          </div>
        ) : reportData ? (
          <div>
            {/* Report Header */}
            <div className="dh-panel">
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                <div>
                  <span className="dh-badge">{reportData.reportType}</span>
                  <h2 style={{ margin: "6px 0 4px", fontSize: "20px", color: "var(--dh-ink)" }}>
                    {reportData.title}
                  </h2>
                  <div style={{ fontSize: "13px", color: "var(--dh-soft)" }}>
                    Department: <strong>{reportData.departmentName}</strong> • Generated At:{" "}
                    {reportData.generatedAt ? new Date(reportData.generatedAt).toLocaleString() : "Just now"}
                  </div>
                </div>
              </div>

              {/* Summary Stats */}
              {reportData.summaryStats && Object.keys(reportData.summaryStats).length > 0 && (
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))",
                    gap: "12px",
                    marginTop: "16px",
                    borderTop: "1px solid #f1f5f9",
                    paddingTop: "14px",
                  }}
                >
                  {Object.entries(reportData.summaryStats).map(([k, v]) => (
                    <div key={k} style={{ background: "#f8fafc", padding: "10px 14px", borderRadius: "8px" }}>
                      <div style={{ fontSize: "11px", color: "var(--dh-faint)", textTransform: "uppercase" }}>
                        {k.replace(/([A-Z])/g, " $1")}
                      </div>
                      <div style={{ fontSize: "16px", fontWeight: "700", color: "var(--dh-ink)", marginTop: "2px" }}>
                        {String(v)}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Table Rows */}
            <div className="dh-panel">
              <div className="dh-table-container">
                <table className="dh-table">
                  <thead>
                    <tr>
                      {reportData.tableHeaders?.map((h, i) => (
                        <th key={i}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {reportData.tableRows && reportData.tableRows.length > 0 ? (
                      reportData.tableRows.map((row, rIdx) => (
                        <tr key={rIdx}>
                          {reportData.tableHeaders.map((h, cIdx) => (
                            <td key={cIdx}>{row[h] != null ? String(row[h]) : "—"}</td>
                          ))}
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td
                          colSpan={reportData.tableHeaders?.length || 1}
                          style={{ textAlign: "center", color: "var(--dh-faint)", padding: "20px" }}
                        >
                          No records matched this report specification.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        ) : null
      ) : (
        /* Shared Leadership Reports Archive */
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="users" size={18} />
              Cross-Leadership Shared Briefings
            </h3>
          </div>
          {sharedReports.length === 0 ? (
            <div style={{ textAlign: "center", color: "var(--dh-faint)", padding: "30px" }}>
              No executive briefings currently shared with Department Leadership.
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              {sharedReports.map((item) => (
                <div
                  key={item.id}
                  style={{
                    padding: "16px",
                    borderRadius: "8px",
                    background: "#f8fafc",
                    border: "1px solid var(--dh-line)",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                    <div>
                      <span className="dh-badge">{item.reportType}</span>
                      <h4 style={{ margin: "6px 0 2px", fontSize: "16px", color: "var(--dh-ink)" }}>
                        {item.title}
                      </h4>
                      <div style={{ fontSize: "12px", color: "var(--dh-faint)" }}>
                        Shared by {item.sharedByName} ({item.sharedWithRole}) •{" "}
                        {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : ""}
                      </div>
                    </div>
                  </div>
                  {item.summaryNotes && (
                    <p style={{ margin: "8px 0 0", fontSize: "13.5px", color: "var(--dh-soft)" }}>
                      {item.summaryNotes}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
