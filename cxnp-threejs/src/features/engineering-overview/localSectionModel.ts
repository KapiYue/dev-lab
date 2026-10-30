import type { Coordinate } from "../../types/domain";
import { simulatedGeologyLayers, type SimulatedGeologyLayer } from "./geologyModel";

export type SectionDataStatus = "available" | "reserved";

export interface SectionDataSource {
  id: "address" | "terrain" | "pipeline" | "borehole" | "strata";
  label: string;
  status: SectionDataStatus;
  provenance: "facility-record" | "simulated" | "external-api";
  endpoint?: string;
  note: string;
}

export interface LocalSectionPoint {
  x: number;
  y: number;
  depth: number;
}

export interface LocalPipelineAlignment {
  id: string;
  name: string;
  diameter: number;
  points: readonly LocalSectionPoint[];
  dataSource: "simulated" | "surveyed";
}

export interface LocalSectionModel {
  facilityId: string;
  facilityName: string;
  center: Coordinate;
  coordinateReferenceSystem: "EPSG:4326";
  footprint: { width: number; depth: number; rotation: number };
  groundElevation: number | null;
  terrainSource: "procedural-preview" | "dem";
  geologyLayers: readonly SimulatedGeologyLayer[];
  pipelines: readonly LocalPipelineAlignment[];
  dataSources: readonly SectionDataSource[];
}

const pipelineByFacility: Record<string, readonly LocalPipelineAlignment[]> = {
  "gujiaqiao-intake": [
    {
      id: "gujiaqiao-inlet-main",
      name: "进水主干管（教学路径）",
      diameter: 3.2,
      dataSource: "simulated",
      points: [
        { x: -76, y: -26, depth: 3.6 },
        { x: -38, y: -12, depth: 4.1 },
        { x: 2, y: -2, depth: 4.8 },
        { x: 42, y: 14, depth: 5.2 },
        { x: 76, y: 28, depth: 5.8 },
      ],
    },
  ],
  "liuxia-river-intake": [
    {
      id: "liuxia-inlet-main",
      name: "进水连接管（教学路径）",
      diameter: 2.6,
      dataSource: "simulated",
      points: [
        { x: -75, y: 27, depth: 4.2 },
        { x: -34, y: 16, depth: 4.6 },
        { x: 4, y: 0, depth: 5.1 },
        { x: 38, y: -13, depth: 5.5 },
        { x: 76, y: -22, depth: 6.1 },
      ],
    },
  ],
};

export function createLocalSectionModel({
  facilityId,
  facilityName,
  center,
}: {
  facilityId: string;
  facilityName: string;
  center: Coordinate;
}): LocalSectionModel {
  if (!Number.isFinite(center[0]) || !Number.isFinite(center[1])) {
    throw new Error("A local section needs a finite WGS84 center coordinate.");
  }

  const pipelines = pipelineByFacility[facilityId] ?? [];
  return {
    facilityId,
    facilityName,
    center,
    coordinateReferenceSystem: "EPSG:4326",
    footprint: { width: 156, depth: 104, rotation: 32 },
    groundElevation: null,
    terrainSource: "procedural-preview",
    geologyLayers: simulatedGeologyLayers,
    pipelines,
    dataSources: [
      {
        id: "address",
        label: "设施地址坐标",
        status: "available",
        provenance: "facility-record",
        note: "使用设施档案中的 WGS84 坐标定位局部剖面中心。",
      },
      {
        id: "terrain",
        label: "地表高程 / DEM",
        status: "reserved",
        provenance: "external-api",
        endpoint: "/api/geology/elevation?lng={lng}&lat={lat}&radius={radius}",
        note: "当前起伏地表为程序化预览，等待真实高程栅格或采样点。",
      },
      {
        id: "pipeline",
        label: "地下管线测绘",
        status: "reserved",
        provenance: "external-api",
        endpoint: "/api/geology/pipelines?facilityId={facilityId}",
        note: "当前管线仅演示空间关系，等待竣工测量或管线 GIS 数据。",
      },
      {
        id: "borehole",
        label: "钻孔柱状数据",
        status: "reserved",
        provenance: "external-api",
        endpoint: "/api/geology/boreholes?lng={lng}&lat={lat}&radius={radius}",
        note: "预留孔口高程、孔深、分层界面和样品指标字段。",
      },
      {
        id: "strata",
        label: "工程勘察地层",
        status: "reserved",
        provenance: "external-api",
        endpoint: "/api/geology/investigation?facilityId={facilityId}",
        note: "当前四层土体均为模拟数据，不能用于设计或施工判断。",
      },
    ],
  };
}

export function validateLocalSectionModel(model: LocalSectionModel) {
  if (model.footprint.width <= 0 || model.footprint.depth <= 0) {
    throw new Error("Local section footprint dimensions must be positive.");
  }
  model.pipelines.forEach((pipeline) => {
    if (pipeline.diameter <= 0 || pipeline.points.length < 2) {
      throw new Error(`Pipeline ${pipeline.id} needs a diameter and at least two points.`);
    }
    if (pipeline.points.some((point) => !Number.isFinite(point.x) || !Number.isFinite(point.y) || point.depth < 0)) {
      throw new Error(`Pipeline ${pipeline.id} contains an invalid local section point.`);
    }
  });
  return model;
}
