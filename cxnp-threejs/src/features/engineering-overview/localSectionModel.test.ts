import { describe, expect, it } from "vitest";
import { createLocalSectionModel, validateLocalSectionModel } from "./localSectionModel";

describe("real-address local section model", () => {
  const section = createLocalSectionModel({
    facilityId: "gujiaqiao-intake",
    facilityName: "顾家桥港进水口",
    center: [120.034, 30.264],
  });

  it("anchors the section to the facility WGS84 coordinate", () => {
    expect(validateLocalSectionModel(section).center).toEqual([120.034, 30.264]);
    expect(section.coordinateReferenceSystem).toBe("EPSG:4326");
    expect(section.footprint).toEqual({ width: 156, depth: 104, rotation: 32 });
  });

  it("keeps simulated preview geometry separate from reserved real sources", () => {
    expect(section.terrainSource).toBe("procedural-preview");
    expect(section.groundElevation).toBeNull();
    expect(section.geologyLayers.every((layer) => layer.dataSource === "simulated")).toBe(true);
    expect(section.pipelines.every((pipeline) => pipeline.dataSource === "simulated")).toBe(true);
    expect(section.dataSources.find((source) => source.id === "address")?.status).toBe("available");
    expect(section.dataSources.filter((source) => source.status === "reserved")).toHaveLength(4);
  });

  it("reserves explicit elevation, pipeline, borehole and investigation endpoints", () => {
    const endpointIds = section.dataSources.filter((source) => source.endpoint).map((source) => source.id);
    expect(endpointIds).toEqual(["terrain", "pipeline", "borehole", "strata"]);
  });

  it("rejects invalid coordinates and pipeline geometry", () => {
    expect(() => createLocalSectionModel({ facilityId: "x", facilityName: "x", center: [Number.NaN, 30] })).toThrow("finite WGS84");
    expect(() => validateLocalSectionModel({
      ...section,
      pipelines: [{ ...section.pipelines[0], points: [{ x: 0, y: 0, depth: 1 }] }],
    })).toThrow("at least two points");
  });
});
