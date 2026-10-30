import { useEffect, useRef, useState } from "react";
import mapboxgl, { type Map as MapboxMap } from "mapbox-gl";
import * as THREE from "three";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader.js";
import { mapboxConfig, mapbox_token } from "../../config/map";
import { drainageLinksGeoJson, overviewZonesGeoJson } from "../../data/overview-zones.mock";
import { southDrainageSites } from "../../data/sites";
import { gcj02ToWgs84 } from "../../utils/coordinateTransform";
import { EmptyState } from "../../components/EmptyState";
import type { DrainageFacility, OverviewZoneProperties } from "../../types/domain";
import type { FeatureCollection } from "geojson";
import { createGeologyLayer, type GeologyCutAxis, type GeologyLayerController } from "./mapboxGeologyLayer";
import { isGeologyFacility } from "./geologyModel";
import { createLocalSectionModel } from "./localSectionModel";

function createModelLayer(coordinate: readonly [number, number], onReady: () => void) {
  const origin = mapboxgl.MercatorCoordinate.fromLngLat({ lng: coordinate[0], lat: coordinate[1] }, 0);
  const scale = origin.meterInMercatorCoordinateUnits() * 3.2;
  let map: MapboxMap;
  let renderer: THREE.WebGLRenderer;
  const camera = new THREE.Camera();
  const scene = new THREE.Scene();
  scene.add(new THREE.AmbientLight(0x9bdfff, 2.2));
  const sun = new THREE.DirectionalLight(0xffffff, 4); sun.position.set(20, -10, 30); scene.add(sun);

  return {
    id: "facility-3d-model", type: "custom", renderingMode: "3d",
    onAdd(activeMap: MapboxMap, gl: WebGLRenderingContext) {
      map = activeMap;
      renderer = new THREE.WebGLRenderer({ canvas: map.getCanvas(), context: gl, antialias: true });
      renderer.autoClear = false;
      new GLTFLoader().load("/models/pump.gltf", (gltf) => {
        gltf.scene.traverse((child) => { if (child instanceof THREE.Mesh) { child.castShadow = true; child.receiveShadow = true; } });
        scene.add(gltf.scene); onReady(); map.triggerRepaint();
      }, undefined, () => {
        const fallback = new THREE.Mesh(new THREE.CylinderGeometry(1.4, 1.4, 2.5, 20), new THREE.MeshStandardMaterial({ color: 0x12bfa8, metalness: .6 }));
        fallback.rotation.x = Math.PI / 2; scene.add(fallback); onReady(); map.triggerRepaint();
      });
    },
    render(_gl: WebGLRenderingContext, matrixOrArgs: any) {
      const rawMatrix = matrixOrArgs?.defaultProjectionData?.mainMatrix ?? matrixOrArgs;
      if (!rawMatrix?.length) return;
      const mapMatrix = new THREE.Matrix4().fromArray(Array.from(rawMatrix));
      const modelMatrix = new THREE.Matrix4()
        .makeTranslation(origin.x, origin.y, origin.z)
        .scale(new THREE.Vector3(scale, -scale, scale))
        .multiply(new THREE.Matrix4().makeRotationX(Math.PI / 2))
        .multiply(new THREE.Matrix4().makeRotationZ(.45));
      camera.projectionMatrix = mapMatrix.multiply(modelMatrix);
      renderer.resetState(); renderer.render(scene, camera);
    },
  } as any;
}

export function MapboxOverview({ sites, zonesVisible, siteType, status, search, selectedId, geologyVisible, geologyOpacity, geologyCutaway, geologyCutAxis, geologyVerticalExaggeration, geologyExploded, onSelectSite, onSelectZone, resetSignal, is3D }: {
  sites: DrainageFacility[]; zonesVisible: boolean; siteType: string; status: string; search: string;
  selectedId: string; geologyVisible: boolean; geologyOpacity: number; geologyCutaway: number;
  geologyCutAxis: GeologyCutAxis; geologyVerticalExaggeration: number; geologyExploded: boolean;
  onSelectSite: (id: string) => void; onSelectZone: (zone: OverviewZoneProperties) => void; resetSignal: number; is3D: boolean;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const mapRef = useRef<MapboxMap | null>(null);
  const geologyLayerRef = useRef<GeologyLayerController | null>(null);
  const [error, setError] = useState("");
  const [modelReady, setModelReady] = useState(false);

  useEffect(() => {
    if (!mapbox_token || !ref.current) return;
    mapboxgl.accessToken = mapbox_token;
    let map: MapboxMap;
    try {
      const center = gcj02ToWgs84(southDrainageSites.xixiWetlandCenter);
      map = new mapboxgl.Map({ container: ref.current, style: mapboxConfig.style, center: [...center], zoom: mapboxConfig.zoom, pitch: mapboxConfig.pitch, bearing: mapboxConfig.bearing, antialias: true, attributionControl: true });
      mapRef.current = map;
      map.addControl(new mapboxgl.NavigationControl({ visualizePitch: true }), "bottom-right");
      map.on("error", (event) => { if (!map.loaded()) setError(event.error?.message ?? "卫星地图加载失败"); });
      map.on("load", () => {
        map.addSource("overview-zones", { type: "geojson", data: overviewZonesGeoJson, promoteId: "id" });
        map.addLayer({ id: "zone-fill", type: "fill", source: "overview-zones", paint: { "fill-color": ["get", "color"], "fill-opacity": .28 } });
        map.addLayer({ id: "zone-outline", type: "line", source: "overview-zones", paint: { "line-color": ["get", "color"], "line-width": 2.4, "line-opacity": .9 } });
        map.addSource("drainage-links", { type: "geojson", data: drainageLinksGeoJson });
        map.addLayer({ id: "drainage-links", type: "line", source: "drainage-links", paint: { "line-color": "#51e9ff", "line-width": 3, "line-opacity": .8, "line-dasharray": [2, 2] } });
        const facilityData: FeatureCollection = { type: "FeatureCollection", features: sites.map((site) => ({ type: "Feature", properties: { id: site.id, name: site.name, status: site.status, type: site.type }, geometry: { type: "Point", coordinates: [...site.wgs84] } })) };
        map.addSource("facilities", { type: "geojson", data: facilityData });
        map.addLayer({ id: "facility-halo", type: "circle", source: "facilities", paint: { "circle-radius": 13, "circle-color": ["match", ["get", "status"], "normal", "#18e3a5", "warning", "#ffb74d", "#71879a"], "circle-opacity": .2, "circle-stroke-width": 1.5, "circle-stroke-color": ["match", ["get", "status"], "normal", "#18e3a5", "warning", "#ffb74d", "#71879a"] } });
        map.addLayer({ id: "facility-point", type: "circle", source: "facilities", paint: { "circle-radius": 5, "circle-color": ["match", ["get", "status"], "normal", "#8affd8", "warning", "#ffd08a", "#a8bbc9"] } });
        map.addLayer(createModelLayer(sites.find((s) => s.id === "southwest-pump")?.wgs84 ?? center, () => setModelReady(true)));
        const initialGeologySite = sites.find((site) => site.id === selectedId && isGeologyFacility(site.id))
          ?? sites.find((site) => site.id === "gujiaqiao-intake");
        if (initialGeologySite) {
          const geologyLayer = createGeologyLayer({
            coordinate: initialGeologySite.wgs84,
            localSection: createLocalSectionModel({
              facilityId: initialGeologySite.id,
              facilityName: initialGeologySite.name,
              center: initialGeologySite.wgs84,
            }),
            visible: geologyVisible && isGeologyFacility(selectedId),
            opacity: geologyOpacity,
            cutaway: geologyCutaway,
            cutAxis: geologyCutAxis,
            verticalExaggeration: geologyVerticalExaggeration,
            exploded: geologyExploded,
          });
          geologyLayerRef.current = geologyLayer;
          map.addLayer(geologyLayer);
        }
        map.on("mouseenter", "zone-fill", () => { map.getCanvas().style.cursor = "pointer"; });
        map.on("mouseleave", "zone-fill", () => { map.getCanvas().style.cursor = ""; });
        map.on("click", "zone-fill", (event) => { const feature = event.features?.[0] as any; if (feature?.properties) onSelectZone(feature.properties as OverviewZoneProperties); });
        map.on("click", "facility-halo", (event) => { const feature = event.features?.[0] as any; const id = feature?.properties?.id; if (id) onSelectSite(id); });
      });
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Mapbox 初始化失败"); }
    return () => { geologyLayerRef.current = null; map?.remove(); mapRef.current = null; };
  }, [sites.length]);

  useEffect(() => {
    const map = mapRef.current;
    const geologyLayer = geologyLayerRef.current;
    if (!map || !geologyLayer) return;
    const site = sites.find((item) => item.id === selectedId);
    const supported = Boolean(site && isGeologyFacility(site.id));
    geologyLayer.setVisible(geologyVisible && supported);
    geologyLayer.setOpacity(geologyOpacity);
    geologyLayer.setCutaway(geologyCutaway);
    geologyLayer.setCutAxis(geologyCutAxis);
    geologyLayer.setVerticalExaggeration(geologyVerticalExaggeration);
    geologyLayer.setExploded(geologyExploded);
    if (site && supported) {
      geologyLayer.setLocalSection(createLocalSectionModel({
        facilityId: site.id,
        facilityName: site.name,
        center: site.wgs84,
      }));
      if (geologyVisible) {
        map.easeTo({
          center: [...site.wgs84],
          zoom: Math.max(map.getZoom(), 17.45),
          pitch: is3D ? 67 : 0,
          bearing: is3D ? -32 : 0,
          duration: 900,
        });
      }
    }
  }, [sites, selectedId, geologyVisible, geologyOpacity, geologyCutaway, geologyCutAxis, geologyVerticalExaggeration, geologyExploded, is3D]);

  useEffect(() => {
    const map = mapRef.current; if (!map?.getLayer("zone-fill")) return;
    map.setLayoutProperty("zone-fill", "visibility", zonesVisible ? "visible" : "none");
    map.setLayoutProperty("zone-outline", "visibility", zonesVisible ? "visible" : "none");
  }, [zonesVisible]);

  useEffect(() => {
    const map = mapRef.current; if (!map?.getLayer("facility-point")) return;
    const filters: any[] = ["all"];
    if (siteType !== "all") filters.push(["==", ["get", "type"], siteType]);
    if (status !== "all") filters.push(["==", ["get", "status"], status]);
    if (search.trim()) filters.push(["in", search.trim(), ["get", "name"]]);
    map.setFilter("facility-point", filters as any); map.setFilter("facility-halo", filters as any);
  }, [siteType, status, search]);

  useEffect(() => {
    const map = mapRef.current; if (!map) return;
    const center = gcj02ToWgs84(southDrainageSites.xixiWetlandCenter);
    map.easeTo({ center: [...center], zoom: mapboxConfig.zoom, pitch: is3D ? mapboxConfig.pitch : 0, bearing: is3D ? mapboxConfig.bearing : 0, duration: 800 });
  }, [resetSignal]);

  useEffect(() => {
    const map = mapRef.current; if (!map) return;
    map.easeTo({ pitch: is3D ? mapboxConfig.pitch : 0, bearing: is3D ? mapboxConfig.bearing : 0, duration: 800 });
  }, [is3D]);

  if (!mapbox_token) return <div className="overview-map fallback-map"><div className="fallback-grid"/><EmptyState title="Mapbox 访问令牌未配置" detail="在 .env.local 中填写 Mapbox public token 后重启开发服务。" action="VITE_MAPBOX_TOKEN=..."/></div>;
  if (error) return <div className="overview-map fallback-map"><div className="fallback-grid"/><EmptyState title="卫星地图加载失败" detail={`${error}。请检查令牌权限和网络连接。`} action="VITE_MAPBOX_TOKEN=..."/></div>;
  return <>
    <div className="overview-map" ref={ref}/>
    {!modelReady && <span className="model-sync-status">重点模型同步中…</span>}
  </>;
}
