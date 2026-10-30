import { monitoringMock } from "./monitoring.mock";
import { gcj02ToWgs84 } from "../utils/coordinateTransform";
import type { Coordinate, DrainageFacility, FacilityType } from "../types/domain";

export const southDrainageSites = {
  gujiaqiaoIntake: [120.043173, 30.257151],
  liuxiaRiverIntake: [120.056725, 30.247730],
  xixiWetlandCenter: [120.067170, 30.264367],
} as const satisfies Record<string, Coordinate>;

const definitions: Array<{
  id: string; name: string; type: FacilityType; description: string; gcj02: Coordinate;
  isMock: boolean; coordinateSource: DrainageFacility["coordinateSource"];
}> = [
  {
    id: "gujiaqiao-intake", name: "顾家桥港进水口", type: "intake",
    description: "城西南排水系统重点进水监测节点。", gcj02: southDrainageSites.gujiaqiaoIntake,
    isMock: false, coordinateSource: "authoritative",
  },
  {
    id: "liuxia-river-intake", name: "留下河进水口", type: "intake",
    description: "承担留下片区来水监测与调度。", gcj02: southDrainageSites.liuxiaRiverIntake,
    isMock: false, coordinateSource: "authoritative",
  },
  {
    id: "southwest-pump", name: "城西南排泵站", type: "pump-station",
    description: "用于演示泵站联动与设备级三维监控。", gcj02: [120.05086, 30.23982],
    isMock: true, coordinateSource: "demo",
  },
  {
    id: "xixi-wetland", name: "西溪湿地", type: "landmark",
    description: "公开范围中心点，仅作为区域空间参照，不代表精确设施。", gcj02: southDrainageSites.xixiWetlandCenter,
    isMock: false, coordinateSource: "public-center",
  },
];

export const drainageSites: DrainageFacility[] = definitions.map((site) => {
  const monitoring = monitoringMock[site.id];
  return {
    ...site,
    wgs84: gcj02ToWgs84(site.gcj02),
    status: monitoring.status,
    metrics: monitoring.metrics,
    updatedAt: monitoring.metrics.updatedAt,
    alarms: monitoring.alarms,
    trend24h: monitoring.trend24h,
    trend7d: monitoring.trend7d,
  };
});
