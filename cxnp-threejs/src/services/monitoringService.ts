import { monitoringMock } from "../data/monitoring.mock";
import type { MonitoringMetrics, TrendPoint } from "../types/domain";

const delay = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms));

export const monitoringService = {
  async getMetrics(siteId: string): Promise<MonitoringMetrics | undefined> {
    await delay(100);
    return structuredClone(monitoringMock[siteId]?.metrics);
  },
  async getTrend(siteId: string, period: "24h" | "7d"): Promise<TrendPoint[]> {
    await delay(120);
    const data = monitoringMock[siteId];
    return structuredClone(period === "24h" ? data?.trend24h ?? [] : data?.trend7d ?? []);
  },
};
