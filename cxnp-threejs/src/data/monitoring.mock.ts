import type { Alarm, FacilityStatus, MonitoringMetrics, TrendPoint } from "../types/domain";

const BASE_TIME = "2026-10-16T17:20:00+08:00";

function buildTrend(count: number, period: "hour" | "day", level: number, flow: number): TrendPoint[] {
  return Array.from({ length: count }, (_, index) => {
    const phase = index / Math.max(1, count - 1);
    const wave = Math.sin(phase * Math.PI * 2 - 1.2);
    return {
      label: period === "hour" ? `${String(index).padStart(2, "0")}:00` : `周${"一二三四五六日"[index]}`,
      level: Number((level + wave * 0.18 + (index % 4) * 0.025).toFixed(2)),
      flow: Number(Math.max(0, flow + wave * 0.42 + (index % 3) * 0.08).toFixed(2)),
      power: Number(Math.max(0, 34 + flow * 8 + wave * 4).toFixed(1)),
    };
  });
}

interface MonitoringSeed {
  status: FacilityStatus;
  level: number;
  flow: number;
  total: number;
  power: number;
  gate: number;
  running: boolean;
  alarmLevel: MonitoringMetrics["alarmLevel"];
  alarmText: string;
  alarms: Alarm[];
}

const seeds: Record<string, MonitoringSeed> = {
  "gujiaqiao-intake": {
    status: "normal", level: 2.41, flow: 3.26, total: 284960, power: 68.4, gate: 72, running: true,
    alarmLevel: "normal", alarmText: "设备运行稳定", alarms: [],
  },
  "liuxia-river-intake": {
    status: "warning", level: 0.68, flow: 1.17, total: 194230, power: 41.7, gate: 38, running: true,
    alarmLevel: "warning", alarmText: "进水池液位偏低", alarms: [
      { id: "alarm-low-level", level: "warning", text: "进水池液位低于 0.70m", time: "17:08" },
    ],
  },
  "southwest-pump": {
    status: "normal", level: 2.86, flow: 4.92, total: 423580, power: 92.6, gate: 81, running: true,
    alarmLevel: "normal", alarmText: "两台泵机联动运行", alarms: [],
  },
  "xixi-wetland": {
    status: "offline", level: 1.53, flow: 0, total: 0, power: 0, gate: 0, running: false,
    alarmLevel: "offline", alarmText: "监测终端通讯中断", alarms: [
      { id: "alarm-comms", level: "offline", text: "水文监测终端通讯异常", time: "16:42" },
    ],
  },
};

export const monitoringMock = Object.fromEntries(
  Object.entries(seeds).map(([id, seed]) => [id, {
    status: seed.status,
    metrics: {
      waterLevel: seed.level,
      instantFlow: seed.flow,
      totalDrainage: seed.total,
      power: seed.power,
      pumpRunning: seed.running,
      gateOpening: seed.gate,
      alarmLevel: seed.alarmLevel,
      alarmText: seed.alarmText,
      updatedAt: BASE_TIME,
      dailyPower: Math.round(seed.power * 13.6),
      dailyCost: Math.round(seed.power * 13.6 * 0.78),
      planDrainage: Math.round(seed.flow * 3600 * 6),
      runningPumps: seed.running ? (seed.flow > 4 ? 2 : 1) : 0,
    } satisfies MonitoringMetrics,
    alarms: seed.alarms,
    trend24h: buildTrend(24, "hour", seed.level, seed.flow),
    trend7d: buildTrend(7, "day", seed.level, seed.flow),
  }]),
);
