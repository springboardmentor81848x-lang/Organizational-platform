import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getHrLearningProgress } from "../../services/hrService.js";

export default function LearningProgressView() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getHrLearningProgress();
      setData(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load learning progress.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const items = data?.progressItems || [];
  const filtered = items.filter((item) => {
    if (statusFilter !== "ALL" && item.status !== statusFilter) return false;
    if (search.trim()) {
      const s = search.toLowerCase();
      const matchEmp = item.employeeName && item.employeeName.toLowerCase().includes(s);
      const matchCourse = item.courseTitle && item.courseTitle.toLowerCase().includes(s);
      const matchDept = item.departmentName && item.departmentName.toLowerCase().includes(s);
      if (!matchEmp && !matchCourse && !matchDept) return false;
    }
    return true;
  });

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Workforce Learning Progress</h3>
            <p className="hr-card-subtitle">
              Monitor individual and cohort engagement, milestone pacing, and certified completion rates
            </p>
          </div>
          <button className="btn-secondary btn-sm" onClick={loadData}>
            <Icon name="refresh" size={14} /> Refresh
          </button>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}

          {/* KPI grid */}
          <div className="hr-stat-grid">
            <div className="hr-stat-card">
              <div className="hr-stat-header">
                <span>Total Enrollments</span>
                <div className="hr-stat-icon blue">
                  <Icon name="book-open" size={18} />
                </div>
              </div>
              <div className="hr-stat-value">{data?.totalCourseEnrollments ?? 0}</div>
              <div className="hr-stat-meta">Active & completed course tracks</div>
            </div>

            <div className="hr-stat-card">
              <div className="hr-stat-header">
                <span>Completed Courses</span>
                <div className="hr-stat-icon emerald">
                  <Icon name="check-circle" size={18} />
                </div>
              </div>
              <div className="hr-stat-value" style={{ color: "var(--hr-emerald)" }}>
                {data?.completedCourseEnrollments ?? 0}
              </div>
              <div className="hr-stat-meta">Successfully passed milestones</div>
            </div>

            <div className="hr-stat-card">
              <div className="hr-stat-header">
                <span>In-Progress Pacing</span>
                <div className="hr-stat-icon amber">
                  <Icon name="clock" size={18} />
                </div>
              </div>
              <div className="hr-stat-value">{data?.inProgressCourseEnrollments ?? 0}</div>
              <div className="hr-stat-meta">Currently actively studying</div>
            </div>

            <div className="hr-stat-card">
              <div className="hr-stat-header">
                <span>Completion Efficiency</span>
                <div className="hr-stat-icon primary">
                  <Icon name="trending" size={18} />
                </div>
              </div>
              <div className="hr-stat-value">{data?.completionRatePercentage ?? 0}%</div>
              <div className="hr-stat-meta">Cohort completion ratio</div>
            </div>
          </div>

          {/* Filters */}
          <div className="hr-filter-bar">
            <input
              type="text"
              className="form-control hr-search-input"
              placeholder="Search by learner, course, or department..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="form-control"
              style={{ width: "auto" }}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Statuses</option>
              <option value="COMPLETED">Completed</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="NOT_STARTED">Not Started</option>
            </select>
          </div>

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading learning progress records...</h4>
            </div>
          ) : filtered.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="book" size={32} />
              <h4>No matching progress records</h4>
              <p>Workforce course enrollments will appear here once initiated.</p>
            </div>
          ) : (
            <div className="hr-table-wrap">
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>Learner</th>
                    <th>Department</th>
                    <th>Course Track</th>
                    <th>Level</th>
                    <th style={{ width: "160px" }}>Progress</th>
                    <th>Status</th>
                    <th>Dates</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((item) => (
                    <tr key={item.enrollmentId}>
                      <td>
                        <div style={{ fontWeight: 600, color: "var(--hr-ink)" }}>
                          {item.employeeName}
                        </div>
                        <div style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                          {item.employeeEmail}
                        </div>
                      </td>
                      <td>{item.departmentName}</td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{item.courseTitle}</div>
                      </td>
                      <td>
                        <span className="status-pill info">{item.courseLevel}</span>
                      </td>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <div
                            style={{
                              flex: 1,
                              height: 8,
                              background: "#e2e8f0",
                              borderRadius: 4,
                              overflow: "hidden",
                            }}
                          >
                            <div
                              style={{
                                width: `${item.progressPct}%`,
                                height: "100%",
                                background:
                                  item.status === "COMPLETED"
                                    ? "var(--hr-emerald)"
                                    : "var(--hr-primary)",
                                borderRadius: 4,
                              }}
                            />
                          </div>
                          <span style={{ fontSize: "12px", fontWeight: 600, minWidth: 32 }}>
                            {item.progressPct}%
                          </span>
                        </div>
                      </td>
                      <td>
                        <span
                          className={`status-pill ${
                            item.status === "COMPLETED"
                              ? "success"
                              : item.status === "IN_PROGRESS"
                              ? "warning"
                              : "neutral"
                          }`}
                        >
                          {item.status}
                        </span>
                      </td>
                      <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                        {item.completedAt
                          ? `Completed ${new Date(item.completedAt).toLocaleDateString()}`
                          : `Enrolled ${new Date(item.enrolledAt).toLocaleDateString()}`}
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
  );
}
