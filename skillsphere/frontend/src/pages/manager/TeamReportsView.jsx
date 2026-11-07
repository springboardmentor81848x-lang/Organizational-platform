import { useState, useEffect } from "react";
import Icon from "../../components/Icons.jsx";
import { getManagerReport, downloadManagerReportCsv } from "../../services/managerService.js";
import { parseApiError } from "../../services/apiClient.js";

const REPORT_TYPES = [
  { id: "TEAM_SKILL_GAP_SUMMARY", label: "1. Team Skill Gap Summary", icon: "alert" },
  { id: "TEAM_PROFICIENCY_MATRIX", label: "2. Team Proficiency Matrix", icon: "chart" },
  { id: "ASSESSMENT_COMPLETION_PERFORMANCE", label: "3. Assessment Completion & Performance", icon: "target" },
  { id: "COURSE_PROGRESS_COMPLIANCE", label: "4. Course Progress & Compliance", icon: "book" },
  { id: "MENTORSHIP_ENGAGEMENT", label: "5. Mentorship Engagement Report", icon: "users" },
  { id: "SKILL_INVENTORY_CATEGORY", label: "6. Skill Inventory by Category", icon: "clipboard" },
  { id: "CRITICAL_SKILL_GAPS", label: "7. At-Risk / Critical Skill Gaps", icon: "shield" },
  { id: "TEAM_DEVELOPMENT_LOG", label: "8. Team Development Activity Log", icon: "clock" },
];

export default function TeamReportsView({ selectedTeamId }) {
  const [reportType, setReportType] = useState("TEAM_SKILL_GAP_SUMMARY");
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [exporting, setExporting] = useState(false);

  const fetchReport = async (type, teamId) => {
    setLoading(true);
    setError("");
    try {
      const data = await getManagerReport(type, teamId);
      setReportData(data);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport(reportType, selectedTeamId);
  }, [reportType, selectedTeamId]);

  const handleExportCsv = async () => {
    setExporting(true);
    try {
      await downloadManagerReportCsv(reportType, selectedTeamId);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setExporting(false);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header no-print">
        <div>
          <span className="eyebrow">Executive Analytics</span>
          <h1>Team Reports & Compliance Exports</h1>
          <p>
            Generate comprehensive, data-backed reports across 8 organizational dimensions.
            Preview live on-screen, export RFC-4180 CSV files, or print/PDF report dossiers.
          </p>
        </div>

        <div style={{ display: "flex", gap: "10px" }}>
          <button className="btn btn--ghost" onClick={handlePrint}>
            <Icon name="print" size={16} /> Print / Save PDF
          </button>
          <button className="btn btn--primary" onClick={handleExportCsv} disabled={exporting || loading}>
            <Icon name="file" size={16} /> {exporting ? "Exporting..." : "Download CSV"}
          </button>
        </div>
      </div>

      {/* Report Type Selector Tabs */}
      <div className="filter-toolbar no-print" style={{ overflowX: "auto", padding: "10px 14px" }}>
        <div style={{ display: "flex", gap: "8px", flexWrap: "nowrap" }}>
          {REPORT_TYPES.map((rt) => (
            <button
              key={rt.id}
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "6px",
                padding: "8px 14px",
                borderRadius: "8px",
                border: "1px solid",
                borderColor: reportType === rt.id ? "#2563eb" : "#e2e8f0",
                background: reportType === rt.id ? "#eff6ff" : "#ffffff",
                color: reportType === rt.id ? "#1d4ed8" : "#475569",
                fontWeight: 700,
                fontSize: "0.82rem",
                whiteSpace: "nowrap",
                cursor: "pointer",
              }}
              onClick={() => setReportType(rt.id)}
            >
              <Icon name={rt.icon} size={15} />
              {rt.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div style={{ padding: "12px 16px", background: "#fee2e2", color: "#991b1b", borderRadius: "10px" }}>
          {error}
        </div>
      )}

      {loading ? (
        <div className="panel" style={{ textAlign: "center", padding: "48px" }}>
          <p className="muted">Compiling report data...</p>
        </div>
      ) : reportData ? (
        <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
          {/* Report Metadata Header */}
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", borderBottom: "1px solid #e2e8f0", paddingBottom: "16px" }}>
            <div>
              <span className="eyebrow">SkillSphere Intelligence Report</span>
              <h2 style={{ fontSize: "1.4rem", margin: "4px 0 6px" }}>{reportData.title}</h2>
              <div style={{ display: "flex", gap: "16px", fontSize: "0.84rem", color: "#64748b" }}>
                <span><strong>Scope:</strong> {reportData.teamName}</span>
                <span><strong>Generated By:</strong> {reportData.managerName}</span>
                <span><strong>Date:</strong> {new Date(reportData.generatedAt).toLocaleString()}</span>
              </div>
            </div>

            <div style={{ textAlign: "right" }}>
              <span className="badge" style={{ fontSize: "0.82rem", padding: "4px 10px" }}>
                {reportData.totalRecords} Records Found
              </span>
            </div>
          </div>

          {/* Summary KPI Cards if present */}
          {reportData.summaryMetrics && Object.keys(reportData.summaryMetrics).length > 0 && (
            <div style={{ display: "flex", gap: "12px", flexWrap: "wrap" }}>
              {Object.entries(reportData.summaryMetrics).map(([key, val]) => (
                <div
                  key={key}
                  style={{
                    padding: "12px 18px",
                    background: "#f8fafc",
                    border: "1px solid #e2e8f0",
                    borderRadius: "10px",
                    minWidth: "140px",
                  }}
                >
                  <small className="muted" style={{ textTransform: "capitalize", display: "block", fontSize: "0.75rem", fontWeight: 700 }}>
                    {key.replace(/([A-Z])/g, " $1")}
                  </small>
                  <strong style={{ fontSize: "1.3rem", color: "#0f172a" }}>{val}</strong>
                </div>
              ))}
            </div>
          )}

          {/* Data Table */}
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  {reportData.columns.map((col, idx) => (
                    <th key={idx}>{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {reportData.rows.length === 0 ? (
                  <tr>
                    <td colSpan={reportData.columns.length} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                      No records match the requested report parameters.
                    </td>
                  </tr>
                ) : (
                  reportData.rows.map((row, rIdx) => (
                    <tr key={rIdx}>
                      {reportData.columns.map((col, cIdx) => (
                        <td key={cIdx}>
                          {col.toLowerCase().includes("priority") || col.toLowerCase().includes("status") ? (
                            <span className="badge">
                              {row[col] != null ? String(row[col]) : "&mdash;"}
                            </span>
                          ) : (
                            <span>{row[col] != null ? String(row[col]) : "&mdash;"}</span>
                          )}
                        </td>
                      ))}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}
    </div>
  );
}
