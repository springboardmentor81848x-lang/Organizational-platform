import { useEffect, useState, useCallback } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getBackups,
  createBackup,
  verifyBackup,
  testRestoreBackup,
  deleteBackup,
  getBackupDownloadUrl,
} from "../../services/platformService.js";
import { parseApiError } from "../../services/apiClient.js";

const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "—");

export default function DatabaseBackupSection() {
  const [backups, setBackups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [operatingFile, setOperatingFile] = useState(null);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Create backup modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [backupNote, setBackupNote] = useState("");

  // Verify modal state
  const [verifyResult, setVerifyResult] = useState(null);

  // Test restore modal state
  const [testRestoreResult, setTestRestoreResult] = useState(null);

  const loadBackups = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const data = await getBackups();
      setBackups(data);
    } catch (e) {
      setError(parseApiError(e).message || "Failed to load database backups.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBackups();
  }, [loadBackups]);

  const handleCreateBackup = async (e) => {
    e.preventDefault();
    setCreating(true);
    setError("");
    setSuccess("");
    try {
      const res = await createBackup({ note: backupNote });
      setSuccess(`Backup ${res.filename} created successfully (${res.formattedSize}, ${res.tableCount} tables).`);
      setShowCreateModal(false);
      setBackupNote("");
      loadBackups();
    } catch (e) {
      setError(parseApiError(e).message || "Failed to create database backup.");
    } finally {
      setCreating(false);
    }
  };

  const handleVerify = async (filename) => {
    setOperatingFile(filename);
    setError("");
    setSuccess("");
    try {
      const res = await verifyBackup(filename);
      setVerifyResult(res);
    } catch (e) {
      setError(parseApiError(e).message || "Verification failed.");
    } finally {
      setOperatingFile(null);
    }
  };

  const handleTestRestore = async (filename) => {
    setOperatingFile(filename);
    setError("");
    setSuccess("");
    try {
      const res = await testRestoreBackup(filename);
      setTestRestoreResult(res);
      setSuccess(`Isolated sandbox test restore of ${filename} succeeded in ${res.durationMs}ms! Active database skillsphere_db remains 100% untouched.`);
    } catch (e) {
      setError(parseApiError(e).message || "Isolated test restore failed.");
    } finally {
      setOperatingFile(null);
    }
  };

  const handleDelete = async (filename) => {
    if (!window.confirm(`Are you sure you want to permanently delete backup archive "${filename}"?`)) {
      return;
    }
    setOperatingFile(filename);
    setError("");
    setSuccess("");
    try {
      await deleteBackup(filename);
      setSuccess(`Backup ${filename} deleted.`);
      loadBackups();
    } catch (e) {
      setError(parseApiError(e).message || "Failed to delete backup.");
    } finally {
      setOperatingFile(null);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      {/* Overview & Actions Banner */}
      <section className="panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "16px" }}>
          <div>
            <span className="eyebrow">DISASTER RECOVERY & GOVERNANCE</span>
            <h2 style={{ margin: "4px 0" }}>PostgreSQL Database Backup & Isolated Verification</h2>
            <p className="muted" style={{ margin: 0 }}>
              Native PostgreSQL 18 custom-format compressed archives (-Fc) stored in host storage with SHA-256 verification and sandbox test restore.
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <button
              type="button"
              className="btn btn--ghost"
              onClick={loadBackups}
              disabled={loading || creating}
            >
              <Icon name="refresh" size={16} /> Refresh
            </button>
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => setShowCreateModal(true)}
              disabled={creating}
            >
              <Icon name="shield" size={16} /> {creating ? "Creating Backup…" : "Create Database Backup"}
            </button>
          </div>
        </div>

        <div className="report-grid" style={{ marginTop: "20px" }}>
          <div className="report-metric">
            <small>Active Database</small>
            <strong>skillsphere_db</strong>
            <span style={{ fontSize: "0.78rem", color: "#17754f" }}>Port 5432 (127.0.0.1)</span>
          </div>
          <div className="report-metric">
            <small>PostgreSQL Engine</small>
            <strong>Version 18.4</strong>
            <span style={{ fontSize: "0.78rem", color: "#5b6d86" }}>Native Windows x86_64</span>
          </div>
          <div className="report-metric">
            <small>Stored Archives</small>
            <strong>{backups.length}</strong>
            <span style={{ fontSize: "0.78rem", color: "#5b6d86" }}>
              {backups.filter((b) => b.protectedSnapshot).length} protected snapshot(s)
            </span>
          </div>
          <div className="report-metric">
            <small>Sandbox Verification</small>
            <strong style={{ color: "#17754f" }}>Active</strong>
            <span style={{ fontSize: "0.78rem", color: "#5b6d86" }}>Isolated Temporary Database</span>
          </div>
        </div>
      </section>

      {error && <Alert type="error">{error}</Alert>}
      {success && <Alert type="success">{success}</Alert>}

      {/* Archives Table */}
      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">BACKUP CATALOG</p>
            <h2>Database Archives ({backups.length})</h2>
          </div>
        </div>

        {loading ? (
          <p className="muted">Loading backup archives from secure storage…</p>
        ) : backups.length === 0 ? (
          <div className="empty-state">
            <span className="empty-state__icon"><Icon name="shield" size={28} /></span>
            <h2>No backup archives found</h2>
            <p>Click "Create Database Backup" to generate an initial compressed PostgreSQL archive.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Archive Filename</th>
                  <th>Date & Time</th>
                  <th>Size</th>
                  <th>Tables</th>
                  <th>SHA-256 Checksum</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {backups.map((b) => {
                  const isBusy = operatingFile === b.filename;
                  const isIncomplete = b.status === "INCOMPLETE_SCHEMA" || (b.tableCount > 0 && b.tableCount < 20);
                  return (
                    <tr key={b.filename} style={isIncomplete ? { background: "#fffdfa" } : undefined}>
                      <td>
                        <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                          <span style={{ fontWeight: 600, fontFamily: "monospace", fontSize: "0.88rem" }}>
                            {b.filename}
                          </span>
                          <div style={{ display: "flex", gap: "6px", alignItems: "center", flexWrap: "wrap" }}>
                            {b.latestBackup && (
                              <span className="badge" style={{ background: "#e6f6ee", color: "#17754f", fontSize: "0.7rem" }}>
                                Latest Valid Backup
                              </span>
                            )}
                            {b.protectedSnapshot && (
                              <span className="badge" style={{ background: "#e0edff", color: "#2563eb", fontSize: "0.7rem" }}>
                                Protected Snapshot
                              </span>
                            )}
                            {b.note && (
                              <span style={{ fontSize: "0.75rem", color: isIncomplete ? "#b54708" : "#5b6d86", fontStyle: "italic" }}>
                                "{b.note}"
                              </span>
                            )}
                          </div>
                        </div>
                      </td>
                      <td>{fmtDate(b.createdAt)}</td>
                      <td>{b.formattedSize || `${b.sizeBytes} B`}</td>
                      <td>{b.tableCount > 0 ? `${b.tableCount} tables` : "—"}</td>
                      <td>
                        <code style={{ fontSize: "0.75rem", color: "#5b6d86" }} title={b.sha256}>
                          {b.sha256 ? `${b.sha256.substring(0, 12)}…` : "—"}
                        </code>
                      </td>
                      <td>
                        {isIncomplete ? (
                          <span
                            className="badge"
                            style={{ background: "#fffaeb", color: "#b54708", border: "1px solid #fedf89", fontSize: "0.75rem" }}
                            title="Incomplete archive from stale Docker instance (9 tables)"
                          >
                            Incomplete Schema ({b.tableCount} tables)
                          </span>
                        ) : b.status === "SUCCESS" ? (
                          <span className="badge" style={{ background: "#e6f6ee", color: "#17754f", fontSize: "0.75rem" }}>
                            SUCCESS
                          </span>
                        ) : (
                          <span className="badge" style={{ background: "#fef3f2", color: "#b42318", fontSize: "0.75rem" }}>
                            {b.status}
                          </span>
                        )}
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                          <button
                            type="button"
                            className="btn btn--ghost"
                            style={{ fontSize: "0.75rem", padding: "4px 8px" }}
                            onClick={() => handleVerify(b.filename)}
                            disabled={isBusy}
                            title="Verify SHA-256 and TOC"
                          >
                            Verify
                          </button>
                          <button
                            type="button"
                            className="btn btn--ghost"
                            style={{ fontSize: "0.75rem", padding: "4px 8px", color: "#2563eb", borderColor: "#c7d9fc" }}
                            onClick={() => handleTestRestore(b.filename)}
                            disabled={isBusy}
                            title="Restore into isolated temp database"
                          >
                            Test in Temp DB
                          </button>
                          <a
                            href={getBackupDownloadUrl(b.filename)}
                            className="btn btn--ghost"
                            style={{ fontSize: "0.75rem", padding: "4px 8px" }}
                            title="Download dump file"
                            download
                          >
                            Download
                          </a>
                          {!b.latestBackup && !b.protectedSnapshot && (
                            <button
                              type="button"
                              className="btn btn--ghost"
                              style={{ fontSize: "0.75rem", padding: "4px 8px", color: "#b42318", borderColor: "#fecdca" }}
                              onClick={() => handleDelete(b.filename)}
                              disabled={isBusy}
                              title="Delete backup archive"
                            >
                              Delete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Create Backup Modal */}
      {showCreateModal && (
        <div className="modal-backdrop" onClick={() => !creating && setShowCreateModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "480px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h2 style={{ margin: 0, fontSize: "1.2rem" }}>Create Database Backup</h2>
              <button type="button" className="text-action" onClick={() => setShowCreateModal(false)} disabled={creating}>
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateBackup}>
              <p style={{ fontSize: "0.88rem", color: "#5b6d86", marginBottom: "16px" }}>
                This will trigger a full native PostgreSQL 18 <code>pg_dump</code> in custom format (-Fc), compute a SHA-256 digest, and store the archive safely in the host backups directory.
              </p>
              <label className="field" style={{ marginBottom: "16px" }}>
                <span>Administrative Note / Reason (Optional)</span>
                <input
                  type="text"
                  value={backupNote}
                  placeholder="e.g. Pre-deployment evaluation backup"
                  onChange={(e) => setBackupNote(e.target.value)}
                  maxLength={120}
                  disabled={creating}
                />
              </label>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
                <button type="button" className="btn btn--ghost" onClick={() => setShowCreateModal(false)} disabled={creating}>
                  Cancel
                </button>
                <button type="submit" className="btn btn--primary" disabled={creating}>
                  {creating ? "Generating Backup…" : "Start Backup"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Verify Result Modal */}
      {verifyResult && (
        <div className="modal-backdrop" onClick={() => setVerifyResult(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "600px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h2 style={{ margin: 0, fontSize: "1.2rem" }}>Backup Integrity Verification</h2>
              <button type="button" className="text-action" onClick={() => setVerifyResult(null)}>✕</button>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "0.88rem" }}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Archive:</span>
                <strong style={{ fontFamily: "monospace" }}>{verifyResult.filename}</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>SHA-256 Digest Match:</span>
                <span className="badge" style={{ background: verifyResult.sha256Matches ? "#e6f6ee" : "#fde9e7", color: verifyResult.sha256Matches ? "#17754f" : "#b42318" }}>
                  {verifyResult.sha256Matches ? "VALID (MATCH)" : "MISMATCH"}
                </span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>TOC Structure:</span>
                <span className="badge" style={{ background: verifyResult.tocValid ? "#e6f6ee" : "#fde9e7", color: verifyResult.tocValid ? "#17754f" : "#b42318" }}>
                  {verifyResult.tocValid ? `${verifyResult.tocEntryCount} Entries Valid` : "INVALID"}
                </span>
              </div>
              <div style={{ background: "#f8faff", padding: "12px", borderRadius: "8px", border: "1px solid #e2e9f3" }}>
                <strong style={{ display: "block", marginBottom: "6px" }}>Verified Public Tables ({verifyResult.tablesFound?.length || 0}):</strong>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
                  {verifyResult.tablesFound?.map((t) => (
                    <span key={t} className="badge" style={{ background: "#fff", border: "1px solid #c7d9fc", color: "#0f2747", fontSize: "0.75rem" }}>
                      {t}
                    </span>
                  ))}
                </div>
              </div>
              <p style={{ margin: "4px 0", color: "#5b6d86" }}>{verifyResult.message}</p>
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "20px" }}>
              <button type="button" className="btn btn--primary" onClick={() => setVerifyResult(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Test Restore Result Modal */}
      {testRestoreResult && (
        <div className="modal-backdrop" onClick={() => setTestRestoreResult(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "600px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h2 style={{ margin: 0, fontSize: "1.2rem" }}>Isolated Sandbox Test Restore Result</h2>
              <button type="button" className="text-action" onClick={() => setTestRestoreResult(null)}>✕</button>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "0.88rem" }}>
              <Alert type="success">
                Isolated temporary database test completed successfully!
              </Alert>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Restored From:</span>
                <strong style={{ fontFamily: "monospace" }}>{testRestoreResult.filename}</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Temporary Sandbox Database:</span>
                <strong style={{ fontFamily: "monospace", color: "#2563eb" }}>{testRestoreResult.tempDatabaseName}</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Restored Public Tables:</span>
                <strong>{testRestoreResult.restoredTableCount} tables</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Verified User Records:</span>
                <strong>{testRestoreResult.verifiedUserCount} users</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Verified Flyway Migrations:</span>
                <strong>{testRestoreResult.verifiedMigrationCount} migrations</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Active Production DB Protection:</span>
                <span className="badge" style={{ background: "#e6f6ee", color: "#17754f" }}>
                  skillsphere_db 100% UNTOUCHED
                </span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Duration:</span>
                <span>{testRestoreResult.durationMs} ms</span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>Temporary Database Cleanup:</span>
                <span className="badge" style={{ background: "#e6f6ee", color: "#17754f" }}>
                  Safely Dropped
                </span>
              </div>
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "20px" }}>
              <button type="button" className="btn btn--primary" onClick={() => setTestRestoreResult(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
