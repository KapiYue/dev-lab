export type Coordinate = readonly [lng: number, lat: number];
export type FacilityStatus = "normal" | "warning" | "offline";
export type AlarmLevel = "normal" | "warning" | "critical" | "offline";
export type FacilityType = "intake" | "pump-station" | "landmark";

export interface Alarm {
  id: string;
  level: AlarmLevel;
  text: string;
  time: string;
  acknowledged?: boolean;
}

export interface TrendPoint {
  label: string;
  level: number;
  flow: number;
  power: number;
}

export interface MonitoringMetrics {
  waterLevel: number;
  instantFlow: number;
  totalDrainage: number;
  power: number;
  pumpRunning: boolean;
  gateOpening: number;
  alarmLevel: AlarmLevel;
  alarmText: string;
  updatedAt: string;
  dailyPower: number;
  dailyCost: number;
  planDrainage: number;
  runningPumps: number;
}

export interface DrainageFacility {
  id: string;
  name: string;
  type: FacilityType;
  description: string;
  status: FacilityStatus;
  gcj02: Coordinate;
  wgs84: Coordinate;
  isMock: boolean;
  coordinateSource: "authoritative" | "public-center" | "demo";
  metrics: MonitoringMetrics;
  updatedAt: string;
  alarms: Alarm[];
  trend24h: TrendPoint[];
  trend7d: TrendPoint[];
}

export interface OverviewZoneProperties {
  id: string;
  name: string;
  area: string;
  deviceCount: number;
  alarmCount: number;
  color: string;
  note: string;
}
