import DatabaseBackupSection from "./DatabaseBackupSection.jsx";
import Icon from "../../components/Icons.jsx";

export default function BackupRecoveryView() {
  return (
    <div className="adm-view-container">
      {/* View Header */}
      <div className="adm-view-header">
        <div>
          <span className="adm-eyebrow">DISASTER RECOVERY & ARCHIVAL GOVERNANCE</span>
          <h1 className="adm-view-title">Database Backup & Disaster Recovery</h1>
          <p className="adm-view-subtitle">
            Automated and on-demand PostgreSQL custom-format backups, SHA-256 integrity verification, and zero-impact isolated sandbox test restores.
          </p>
        </div>
      </div>

      {/* Safety & Isolation Notice Banner */}
      <div className="adm-card" style={{ borderLeft: "4px solid #0f766e", padding: "16px 20px" }}>
        <div style={{ display: "flex", alignItems: "flex-start", gap: "12px" }}>
          <Icon name="shield" size={20} style={{ color: "#0f766e", marginTop: "2px" }} />
          <div>
            <h3 style={{ margin: "0 0 4px 0", fontSize: "14px", color: "#0f172a", fontWeight: 600 }}>
              Zero-Downtime Sandbox Isolation Architecture
            </h3>
            <p style={{ margin: 0, fontSize: "13px", color: "#475569", lineHeight: 1.5 }}>
              Database backups are created using native <code className="adm-code">pg_dump -Fc</code> compression. Test restores are executed strictly inside an isolated temporary sandbox schema or database with dynamic 46+ table completeness checks. The production database <code className="adm-code">skillsphere_db</code> is 100% protected against unintentional overwrites.
            </p>
          </div>
        </div>
      </div>

      {/* Embedded Complete Database Backup Section */}
      <DatabaseBackupSection />
    </div>
  );
}
