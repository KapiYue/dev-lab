import { gcj02ToWgs84 } from "../utils/coordinateTransform";
import type { Coordinate, OverviewZoneProperties } from "../types/domain";
import type { FeatureCollection, LineString, Polygon } from "geojson";

const gcjZones: Array<{ properties: OverviewZoneProperties; ring: Coordinate[] }> = [
  {
    properties: { id: "west-intake", name: "西片进水控制区", area: "3.42 km²", deviceCount: 14, alarmCount: 1, color: "#12d8a0", note: "演示范围，待工程边界数据校准" },
    ring: [[120.035,30.250],[120.046,30.246],[120.052,30.254],[120.048,30.264],[120.037,30.264],[120.035,30.250]],
  },
  {
    properties: { id: "central-pump", name: "泵站联动调度区", area: "2.85 km²", deviceCount: 11, alarmCount: 0, color: "#16b8e6", note: "演示范围，待工程边界数据校准" },
    ring: [[120.045,30.235],[120.057,30.232],[120.063,30.241],[120.056,30.250],[120.046,30.246],[120.045,30.235]],
  },
  {
    properties: { id: "wetland-buffer", name: "湿地生态缓冲区", area: "4.18 km²", deviceCount: 8, alarmCount: 1, color: "#9bd94b", note: "演示范围，待工程边界数据校准" },
    ring: [[120.055,30.252],[120.065,30.248],[120.077,30.255],[120.077,30.269],[120.063,30.273],[120.052,30.264],[120.055,30.252]],
  },
];

export const overviewZonesGeoJson: FeatureCollection<Polygon, OverviewZoneProperties> = {
  type: "FeatureCollection",
  features: gcjZones.map(({ properties, ring }) => ({
    type: "Feature",
    properties,
    geometry: { type: "Polygon", coordinates: [ring.map((point) => [...gcj02ToWgs84(point)])] },
  })),
};

export const drainageLinksGeoJson: FeatureCollection<LineString> = {
  type: "FeatureCollection",
  features: [
    {
      type: "Feature",
      properties: { name: "顾家桥—城西南排联动线" },
      geometry: { type: "LineString", coordinates: ([[120.043173,30.257151],[120.05086,30.23982]] as Coordinate[]).map((p) => [...gcj02ToWgs84(p)]) },
    },
    {
      type: "Feature",
      properties: { name: "留下河—城西南排联动线" },
      geometry: { type: "LineString", coordinates: ([[120.056725,30.24773],[120.05086,30.23982]] as Coordinate[]).map((p) => [...gcj02ToWgs84(p)]) },
    },
  ],
};
