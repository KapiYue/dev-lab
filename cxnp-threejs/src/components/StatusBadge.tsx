import type { FacilityStatus } from "../types/domain";

const labels: Record<FacilityStatus, string> = { normal: "正常", warning: "预警", offline: "离线" };

export function StatusBadge({ status, pulse = false }: { status: FacilityStatus; pulse?: boolean }) {
  return <span className={`status-badge status-${status}`}><i className={pulse ? "pulse" : ""}/>{labels[status]}</span>;
}
