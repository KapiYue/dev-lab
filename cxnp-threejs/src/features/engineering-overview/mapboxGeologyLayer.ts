import mapboxgl, { type CustomLayerInterface, type Map as MapboxMap } from "mapbox-gl";
import * as THREE from "three";
import type { Coordinate } from "../../types/domain";
import {
  clampGeologyOpacity,
  getCutawayThreshold,
  simulatedGeologyLayers,
} from "./geologyModel";
import type { LocalSectionModel } from "./localSectionModel";

const FOOTPRINT_WIDTH = 156;
const FOOTPRINT_DEPTH = 104;
const TOTAL_DEPTH = simulatedGeologyLayers.at(-1)?.bottomDepth ?? 20;

export type GeologyCutAxis = "x" | "y" | "z" | "diagonal";

export interface GeologyLayerController extends CustomLayerInterface {
  setCoordinate(coordinate: Coordinate): void;
  setLocalSection(section: LocalSectionModel): void;
  setVisible(visible: boolean): void;
  setOpacity(opacity: number): void;
  setCutaway(percent: number): void;
  setCutAxis(axis: GeologyCutAxis): void;
  setVerticalExaggeration(value: number): void;
  setExploded(exploded: boolean): void;
}

export function createGeologyLayer({
  coordinate,
  localSection,
  visible,
  opacity,
  cutaway,
  cutAxis = "y",
  verticalExaggeration = 2.4,
  exploded = false,
}: {
  coordinate: Coordinate;
  localSection: LocalSectionModel;
  visible: boolean;
  opacity: number;
  cutaway: number;
  cutAxis?: GeologyCutAxis;
  verticalExaggeration?: number;
  exploded?: boolean;
}): GeologyLayerController {
  let map: MapboxMap | null = null;
  let renderer: THREE.WebGLRenderer | null = null;
  let origin = mapboxgl.MercatorCoordinate.fromLngLat({ lng: coordinate[0], lat: coordinate[1] }, 0);
  let mercatorScale = origin.meterInMercatorCoordinateUnits();
  let isVisible = visible;
  let currentOpacity = clampGeologyOpacity(opacity);
  let currentCutaway = cutaway;
  let currentCutAxis = cutAxis;
  let currentVerticalExaggeration = verticalExaggeration;
  let isExploded = exploded;

  const camera = new THREE.Camera();
  const scene = new THREE.Scene();
  const group = new THREE.Group();
  const infrastructureGroup = new THREE.Group();
  scene.add(group);
  group.add(infrastructureGroup);
  scene.add(new THREE.HemisphereLight(0xd9f7ff, 0x33291f, 2.3));
  const sun = new THREE.DirectionalLight(0xffffff, 3.4);
  sun.position.set(-40, -30, 70);
  scene.add(sun);

  const clippingPlane = new THREE.Plane(new THREE.Vector3(0, 1, 0), 0);
  const fillMaterials: THREE.MeshStandardMaterial[] = [];
  const edgeMaterials: THREE.LineBasicMaterial[] = [];
  const layerObjects: Array<{ mesh: THREE.Mesh; edges: THREE.LineSegments; baseZ: number; index: number }> = [];
  const infrastructureMaterials: THREE.Material[] = [];

  const clearInfrastructure = () => {
    infrastructureGroup.children.slice().forEach((object) => {
      infrastructureGroup.remove(object);
      if (object instanceof THREE.Mesh || object instanceof THREE.Line || object instanceof THREE.LineLoop) {
        object.geometry.dispose();
        if (Array.isArray(object.material)) object.material.forEach((material) => material.dispose());
        else object.material.dispose();
      }
    });
    infrastructureMaterials.length = 0;
  };

  const rebuildInfrastructure = (section: LocalSectionModel) => {
    clearInfrastructure();
    const halfWidth = section.footprint.width / 2;
    const halfDepth = section.footprint.depth / 2;
    const boundaryGeometry = new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(-halfWidth, -halfDepth, 0.5),
      new THREE.Vector3(halfWidth, -halfDepth, 0.5),
      new THREE.Vector3(halfWidth, halfDepth, 0.5),
      new THREE.Vector3(-halfWidth, halfDepth, 0.5),
    ]);
    const boundaryMaterial = new THREE.LineBasicMaterial({ color: 0x56e8ff, transparent: true, opacity: 0.9 });
    const boundary = new THREE.LineLoop(boundaryGeometry, boundaryMaterial);
    boundary.renderOrder = 30;
    infrastructureGroup.add(boundary);
    infrastructureMaterials.push(boundaryMaterial);

    section.pipelines.forEach((pipeline) => {
      const path = new THREE.CatmullRomCurve3(
        pipeline.points.map((point) => new THREE.Vector3(point.x, point.y, -point.depth)),
        false,
        "centripetal",
      );
      const shellMaterial = new THREE.MeshStandardMaterial({
        color: 0x8da8b5,
        metalness: 0.68,
        roughness: 0.28,
        transparent: true,
        opacity: 0.72,
        side: THREE.DoubleSide,
        clippingPlanes: currentCutaway > 0 ? [clippingPlane] : [],
      });
      const flowMaterial = new THREE.MeshBasicMaterial({
        color: 0x32ddff,
        transparent: true,
        opacity: 0.88,
        clippingPlanes: currentCutaway > 0 ? [clippingPlane] : [],
      });
      const shell = new THREE.Mesh(new THREE.TubeGeometry(path, 72, pipeline.diameter / 2, 16, false), shellMaterial);
      const flow = new THREE.Mesh(new THREE.TubeGeometry(path, 72, pipeline.diameter * 0.29, 12, false), flowMaterial);
      shell.renderOrder = 24;
      flow.renderOrder = 25;
      infrastructureGroup.add(shell, flow);
      infrastructureMaterials.push(shellMaterial, flowMaterial);
    });

    const boreholeMaterial = new THREE.MeshBasicMaterial({
      color: 0xffd470,
      transparent: true,
      opacity: 0.9,
      clippingPlanes: currentCutaway > 0 ? [clippingPlane] : [],
    });
    const borehole = new THREE.Mesh(new THREE.CylinderGeometry(0.65, 0.65, TOTAL_DEPTH + 4, 10), boreholeMaterial);
    borehole.rotation.x = Math.PI / 2;
    borehole.position.z = -(TOTAL_DEPTH - 4) / 2;
    borehole.renderOrder = 26;
    infrastructureGroup.add(borehole);
    infrastructureMaterials.push(boreholeMaterial);
  };

  simulatedGeologyLayers.forEach((layer, index) => {
    const thickness = layer.bottomDepth - layer.topDepth;
    const geometry = new THREE.BoxGeometry(FOOTPRINT_WIDTH, FOOTPRINT_DEPTH, thickness);
    const material = new THREE.MeshStandardMaterial({
      color: layer.color,
      roughness: 0.86,
      metalness: 0.02,
      transparent: true,
      opacity: currentOpacity,
      side: THREE.DoubleSide,
      clippingPlanes: currentCutaway > 0 ? [clippingPlane] : [],
    });
    const mesh = new THREE.Mesh(geometry, material);
    mesh.position.z = -(layer.topDepth + layer.bottomDepth) / 2;
    mesh.renderOrder = index;
    group.add(mesh);
    fillMaterials.push(material);

    const edgeMaterial = new THREE.LineBasicMaterial({
      color: new THREE.Color(layer.color).offsetHSL(0, 0.05, 0.2),
      transparent: true,
      opacity: Math.min(1, currentOpacity + 0.2),
      clippingPlanes: currentCutaway > 0 ? [clippingPlane] : [],
    });
    const edges = new THREE.LineSegments(new THREE.EdgesGeometry(geometry), edgeMaterial);
    edges.position.copy(mesh.position);
    edges.renderOrder = index + 10;
    group.add(edges);
    edgeMaterials.push(edgeMaterial);
    layerObjects.push({ mesh, edges, baseZ: mesh.position.z, index });
  });

  const surfaceGeometry = new THREE.PlaneGeometry(FOOTPRINT_WIDTH + 4, FOOTPRINT_DEPTH + 4, 28, 20);
  const surfacePositions = surfaceGeometry.attributes.position;
  for (let index = 0; index < surfacePositions.count; index += 1) {
    const x = surfacePositions.getX(index);
    const y = surfacePositions.getY(index);
    const height = Math.sin(x * 0.095) * 1.35 + Math.cos(y * 0.12) * 0.9 + Math.sin((x + y) * 0.045) * 1.1;
    surfacePositions.setZ(index, height);
  }
  surfaceGeometry.computeVertexNormals();
  const surfaceMaterial = new THREE.MeshStandardMaterial({
    color: 0xb3a52a,
    transparent: true,
    opacity: 0.82,
    side: THREE.DoubleSide,
    roughness: 0.94,
    metalness: 0.02,
    clippingPlanes: currentCutaway > 0 ? [clippingPlane] : [],
  });
  const surface = new THREE.Mesh(surfaceGeometry, surfaceMaterial);
  surface.position.z = 0.15;
  surface.renderOrder = 20;
  group.add(surface);

  const gridMaterial = new THREE.LineBasicMaterial({ color: 0x9ec9d4, transparent: true, opacity: 0.58 });
  const gridPoints: THREE.Vector3[] = [];
  const gridStep = 13;
  for (let x = -FOOTPRINT_WIDTH / 2; x <= FOOTPRINT_WIDTH / 2 + 0.01; x += gridStep) {
    gridPoints.push(new THREE.Vector3(x, -FOOTPRINT_DEPTH / 2 - 12, -TOTAL_DEPTH - 1), new THREE.Vector3(x, FOOTPRINT_DEPTH / 2 + 12, -TOTAL_DEPTH - 1));
    gridPoints.push(new THREE.Vector3(x, FOOTPRINT_DEPTH / 2 + 8, -TOTAL_DEPTH - 1), new THREE.Vector3(x, FOOTPRINT_DEPTH / 2 + 8, 8));
  }
  for (let y = -FOOTPRINT_DEPTH / 2 - 12; y <= FOOTPRINT_DEPTH / 2 + 12; y += gridStep) {
    gridPoints.push(new THREE.Vector3(-FOOTPRINT_WIDTH / 2 - 10, y, -TOTAL_DEPTH - 1), new THREE.Vector3(FOOTPRINT_WIDTH / 2 + 10, y, -TOTAL_DEPTH - 1));
  }
  for (let z = -TOTAL_DEPTH - 1; z <= 8; z += 4) {
    gridPoints.push(new THREE.Vector3(-FOOTPRINT_WIDTH / 2 - 10, FOOTPRINT_DEPTH / 2 + 8, z), new THREE.Vector3(FOOTPRINT_WIDTH / 2 + 10, FOOTPRINT_DEPTH / 2 + 8, z));
    gridPoints.push(new THREE.Vector3(-FOOTPRINT_WIDTH / 2 - 10, -FOOTPRINT_DEPTH / 2 - 12, z), new THREE.Vector3(-FOOTPRINT_WIDTH / 2 - 10, FOOTPRINT_DEPTH / 2 + 8, z));
  }
  const gridGeometry = new THREE.BufferGeometry().setFromPoints(gridPoints);
  const grid = new THREE.LineSegments(gridGeometry, gridMaterial);
  grid.renderOrder = -1;
  group.add(grid);

  const updateLayerTransforms = () => {
    group.scale.z = Math.min(10, Math.max(1, currentVerticalExaggeration));
    layerObjects.forEach(({ mesh, edges, baseZ, index }) => {
      const gap = isExploded ? index * 2.2 : 0;
      mesh.position.z = baseZ - gap;
      edges.position.z = baseZ - gap;
    });
    surface.position.z = isExploded ? 3.2 : 0.15;
  };

  const updateCutaway = () => {
    if (currentCutAxis === "x") {
      clippingPlane.normal.set(1, 0, 0);
      clippingPlane.constant = -getCutawayThreshold(currentCutaway, FOOTPRINT_WIDTH);
    } else if (currentCutAxis === "z") {
      clippingPlane.normal.set(0, 0, -1);
      clippingPlane.constant = -TOTAL_DEPTH * (currentCutaway / 100) * currentVerticalExaggeration;
    } else if (currentCutAxis === "diagonal") {
      clippingPlane.normal.set(1, 1, 0).normalize();
      clippingPlane.constant = -getCutawayThreshold(currentCutaway, Math.hypot(FOOTPRINT_WIDTH, FOOTPRINT_DEPTH));
    } else {
      clippingPlane.normal.set(0, 1, 0);
      clippingPlane.constant = -getCutawayThreshold(currentCutaway, FOOTPRINT_DEPTH);
    }
    const planes = currentCutaway > 0 ? [clippingPlane] : [];
    fillMaterials.forEach((material) => { material.clippingPlanes = planes; material.needsUpdate = true; });
    edgeMaterials.forEach((material) => { material.clippingPlanes = planes; material.needsUpdate = true; });
    surfaceMaterial.clippingPlanes = planes;
    surfaceMaterial.needsUpdate = true;
    infrastructureMaterials.forEach((material) => {
      if (material instanceof THREE.Material && "clippingPlanes" in material) {
        (material as THREE.MeshBasicMaterial).clippingPlanes = planes;
        material.needsUpdate = true;
      }
    });
  };
  rebuildInfrastructure(localSection);
  updateLayerTransforms();
  updateCutaway();

  const controller: GeologyLayerController = {
    id: "simulated-geology-volume",
    type: "custom",
    renderingMode: "3d",
    onAdd(activeMap, gl) {
      map = activeMap;
      renderer = new THREE.WebGLRenderer({
        canvas: activeMap.getCanvas(),
        context: gl,
        antialias: true,
      });
      renderer.autoClear = false;
      renderer.localClippingEnabled = true;
    },
    render(gl, matrixOrArgs: unknown) {
      if (!renderer || !isVisible) return;
      const renderArgs = matrixOrArgs as { defaultProjectionData?: { mainMatrix?: ArrayLike<number> } } | ArrayLike<number>;
      const rawMatrix = "defaultProjectionData" in Object(renderArgs)
        ? (renderArgs as { defaultProjectionData?: { mainMatrix?: ArrayLike<number> } }).defaultProjectionData?.mainMatrix
        : renderArgs as ArrayLike<number>;
      if (!rawMatrix || typeof rawMatrix.length !== "number") return;
      const mapMatrix = new THREE.Matrix4().fromArray(Array.from(rawMatrix));
      const modelMatrix = new THREE.Matrix4()
        .makeTranslation(origin.x, origin.y, origin.z)
        .scale(new THREE.Vector3(mercatorScale, -mercatorScale, mercatorScale));
      camera.projectionMatrix = mapMatrix.multiply(modelMatrix);
      renderer.resetState();
      renderer.clearDepth();
      renderer.render(scene, camera);
      gl.flush();
    },
    onRemove() {
      clearInfrastructure();
      group.traverse((object) => {
        if (object instanceof THREE.Mesh || object instanceof THREE.LineSegments) {
          object.geometry.dispose();
        }
      });
      fillMaterials.forEach((material) => material.dispose());
      edgeMaterials.forEach((material) => material.dispose());
      surfaceMaterial.dispose();
      gridMaterial.dispose();
      renderer?.dispose();
      renderer = null;
      map = null;
    },
    setCoordinate(nextCoordinate) {
      origin = mapboxgl.MercatorCoordinate.fromLngLat({ lng: nextCoordinate[0], lat: nextCoordinate[1] }, 0);
      mercatorScale = origin.meterInMercatorCoordinateUnits();
      map?.triggerRepaint();
    },
    setLocalSection(nextSection) {
      origin = mapboxgl.MercatorCoordinate.fromLngLat({ lng: nextSection.center[0], lat: nextSection.center[1] }, 0);
      mercatorScale = origin.meterInMercatorCoordinateUnits();
      rebuildInfrastructure(nextSection);
      updateCutaway();
      map?.triggerRepaint();
    },
    setVisible(nextVisible) {
      isVisible = nextVisible;
      map?.triggerRepaint();
    },
    setOpacity(nextOpacity) {
      currentOpacity = clampGeologyOpacity(nextOpacity);
      fillMaterials.forEach((material) => { material.opacity = currentOpacity; });
      edgeMaterials.forEach((material) => { material.opacity = Math.min(1, currentOpacity + 0.2); });
      surfaceMaterial.opacity = Math.min(0.95, 0.45 + currentOpacity * 0.5);
      map?.triggerRepaint();
    },
    setCutaway(nextCutaway) {
      currentCutaway = Math.min(100, Math.max(0, nextCutaway));
      updateCutaway();
      map?.triggerRepaint();
    },
    setCutAxis(nextAxis) {
      currentCutAxis = nextAxis;
      updateCutaway();
      map?.triggerRepaint();
    },
    setVerticalExaggeration(nextValue) {
      currentVerticalExaggeration = Math.min(10, Math.max(1, nextValue));
      updateLayerTransforms();
      updateCutaway();
      map?.triggerRepaint();
    },
    setExploded(nextExploded) {
      isExploded = nextExploded;
      updateLayerTransforms();
      map?.triggerRepaint();
    },
  };

  return controller;
}
