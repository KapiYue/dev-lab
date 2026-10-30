import AMapLoader from "@amap/amap-jsapi-loader";
import { useEffect, useRef, useState } from "react";
import { amapSecurityJsCode, cityMapConfig, cxnp_web_amap } from "../../config/map";
import type { DrainageFacility } from "../../types/domain";
import { EmptyState } from "../../components/EmptyState";

declare global { interface Window { _AMapSecurityConfig?: { securityJsCode: string } } }

const colors = { normal: "#1ee6ab", warning: "#ffb84d", offline: "#698398" };

export function CityMap({ sites, selectedId, onSelect }: { sites: DrainageFacility[]; selectedId: string; onSelect: (id: string) => void }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<any>(null);
  const markersRef = useRef<Map<string, any>>(new Map());
  const [error, setError] = useState("");
  const siteSignature = sites.map((site) => site.id).join("|");

  useEffect(() => {
    if (!cxnp_web_amap) return;
    let cancelled = false;
    if (amapSecurityJsCode) window._AMapSecurityConfig = { securityJsCode: amapSecurityJsCode };
    AMapLoader.load({ key: cxnp_web_amap, version: "2.0", plugins: ["AMap.Scale", "AMap.ToolBar"] })
      .then((AMap) => {
        if (cancelled || !containerRef.current) return;
        const map = new AMap.Map(containerRef.current, {
          center: cityMapConfig.center,
          zoom: cityMapConfig.zoom,
          mapStyle: "amap://styles/darkblue",
          viewMode: "3D",
          pitch: 34,
          showLabel: true,
        });
        map.addControl(new AMap.Scale({ position: "LB" }));
        map.addControl(new AMap.ToolBar({ position: { right: "18px", bottom: "32px" } }));
        sites.forEach((site) => {
          const el = document.createElement("button");
          el.className = `amap-facility-marker ${site.id === selectedId ? "selected" : ""}`;
          el.style.setProperty("--marker-color", colors[site.status]);
          el.innerHTML = `<span class="marker-pulse"></span><span class="marker-core"></span><label>${site.name}</label>`;
          el.setAttribute("aria-label", `选择${site.name}`);
          el.addEventListener("click", () => onSelect(site.id));
          const marker = new AMap.Marker({ position: site.gcj02, content: el, offset: new AMap.Pixel(-12, -12), zIndex: site.id === selectedId ? 150 : 100 });
          map.add(marker);
          markersRef.current.set(site.id, marker);
        });
        mapRef.current = map;
      })
      .catch((reason) => setError(reason instanceof Error ? reason.message : "地图资源加载失败"));
    return () => { cancelled = true; markersRef.current.clear(); mapRef.current?.destroy(); mapRef.current = null; };
  }, [siteSignature]);

  useEffect(() => {
    if (!mapRef.current) return;
    const site = sites.find((item) => item.id === selectedId);
    if (!site) return;
    mapRef.current.setZoomAndCenter(14.4, site.gcj02, true, 550);
    markersRef.current.forEach((marker, id) => {
      marker.getContent()?.classList.toggle("selected", id === selectedId);
      const current = sites.find((item) => item.id === id);
      if (current) marker.getContent()?.style.setProperty("--marker-color", colors[current.status]);
      marker.setzIndex(id === selectedId ? 150 : 100);
    });
  }, [selectedId, sites]);

  if (!cxnp_web_amap) return <div className="map-stage fallback-map"><div className="fallback-grid"/>
    <EmptyState title="高德地图密钥未配置" detail="复制 .env.example 为 .env.local，填写高德 Web JS API Key 后重启开发服务。" action="VITE_CXNP_WEB_AMAP=..." />
  </div>;
  if (error) return <div className="map-stage fallback-map"><div className="fallback-grid"/>
    <EmptyState title="高德地图加载失败" detail={`${error}。请检查密钥、域名白名单与网络连接。`} action={amapSecurityJsCode ? undefined : "可选：VITE_AMAP_SECURITY_JS_CODE=..."}/>
  </div>;
  return <div className="map-stage" ref={containerRef} aria-label="城西南排水设施城市地图"/>;
}
