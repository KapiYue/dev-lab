export interface SimulatedGeologyLayer {
  id: string;
  name: string;
  topDepth: number;
  bottomDepth: number;
  color: number;
  description: string;
  dataSource: "simulated";
}

export const GEOLOGY_FACILITY_IDS = ["gujiaqiao-intake", "liuxia-river-intake"] as const;

export const simulatedGeologyLayers: readonly SimulatedGeologyLayer[] = [
  {
    id: "made-ground",
    name: "杂填土",
    topDepth: 0,
    bottomDepth: 2.4,
    color: 0x8d6e52,
    description: "地表回填与人工扰动层",
    dataSource: "simulated",
  },
  {
    id: "silty-clay",
    name: "粉质黏土",
    topDepth: 2.4,
    bottomDepth: 6.8,
    color: 0xc59b61,
    description: "中等压缩性的黏性土层",
    dataSource: "simulated",
  },
  {
    id: "silt",
    name: "淤泥质土",
    topDepth: 6.8,
    bottomDepth: 12.6,
    color: 0x677d72,
    description: "高含水量软土模拟层",
    dataSource: "simulated",
  },
  {
    id: "sand-gravel",
    name: "砂砾持力层",
    topDepth: 12.6,
    bottomDepth: 19.5,
    color: 0x7b6956,
    description: "密实砂砾与持力层模拟",
    dataSource: "simulated",
  },
] as const;

export function isGeologyFacility(id: string): id is (typeof GEOLOGY_FACILITY_IDS)[number] {
  return GEOLOGY_FACILITY_IDS.some((facilityId) => facilityId === id);
}

export function validateGeologyLayers(layers: readonly SimulatedGeologyLayer[]) {
  if (layers.length < 3 || layers.length > 5) {
    throw new Error("A simulated geology volume needs between three and five layers.");
  }

  layers.forEach((layer, index) => {
    if (layer.topDepth < 0 || layer.bottomDepth <= layer.topDepth) {
      throw new Error(`Geology layer ${layer.id} needs a positive depth interval.`);
    }
    if (index > 0 && layer.topDepth !== layers[index - 1].bottomDepth) {
      throw new Error("Geology depth intervals must be continuous.");
    }
  });

  return layers;
}

export function clampGeologyOpacity(value: number) {
  if (!Number.isFinite(value)) return 0.68;
  return Math.min(0.92, Math.max(0.2, value));
}

export function getCutawayThreshold(percent: number, footprintDepth: number) {
  if (!Number.isFinite(percent) || !Number.isFinite(footprintDepth) || footprintDepth <= 0) {
    throw new Error("Cutaway inputs must be finite and the footprint depth must be positive.");
  }
  const normalized = Math.min(100, Math.max(0, percent)) / 100;
  return -footprintDepth / 2 + footprintDepth * normalized;
}

validateGeologyLayers(simulatedGeologyLayers);
