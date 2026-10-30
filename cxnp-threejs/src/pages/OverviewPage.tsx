import { lazy, Suspense, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AppHeader } from "../components/AppHeader";
import { Icon } from "../components/Icon";
import { StatusBadge } from "../components/StatusBadge";
import { overviewZonesGeoJson } from "../data/overview-zones.mock";
import { usePlatform } from "../state/PlatformContext";
import type { OverviewZoneProperties } from "../types/domain";
import { isGeologyFacility, simulatedGeologyLayers } from "../features/engineering-overview/geologyModel";
import type { GeologyCutAxis } from "../features/engineering-overview/mapboxGeologyLayer";
import { createLocalSectionModel } from "../features/engineering-overview/localSectionModel";

const MapboxOverview = lazy(() => import("../features/engineering-overview/MapboxOverview").then((module) => ({ default: module.MapboxOverview })));

const cutAxisLabels: Array<{ axis: GeologyCutAxis; label: string }> = [
  { axis: "x", label: "X 轴" },
  { axis: "y", label: "Y 轴" },
  { axis: "z", label: "Z 轴" },
  { axis: "diagonal", label: "任意线" },
];

export default function OverviewPage() {
  const { sites, selectedId, setSelectedId } = usePlatform();
  const [search, setSearch] = useState("");
  const [siteType, setSiteType] = useState("all");
  const [status, setStatus] = useState("all");
  const [zonesVisible, setZonesVisible] = useState(true);
  const [is3D, setIs3D] = useState(true);
  const [geologyVisible, setGeologyVisible] = useState(false);
  const [geologyOpacity, setGeologyOpacity] = useState(0.82);
  const [geologyCutaway, setGeologyCutaway] = useState(38);
  const [geologyCutAxis, setGeologyCutAxis] = useState<GeologyCutAxis>("y");
  const [geologyVerticalExaggeration, setGeologyVerticalExaggeration] = useState(2.4);
  const [geologyExploded, setGeologyExploded] = useState(false);
  const [showDepth, setShowDepth] = useState(true);
  const [geologyBackdrop, setGeologyBackdrop] = useState(true);
  const [wmsUrl, setWmsUrl] = useState("");
  const [wmsNotice, setWmsNotice] = useState("");
  const [resetSignal, setResetSignal] = useState(0);
  const [zone, setZone] = useState<OverviewZoneProperties>(() => overviewZonesGeoJson.features[0].properties);
  const selected = sites.find((site) => site.id === selectedId) ?? sites[0];
  const geologySupported = Boolean(selected && isGeologyFacility(selected.id));
  const geologyMode = geologyVisible && geologySupported;
  const filtered = useMemo(() => sites.filter((item) => (siteType === "all" || item.type === siteType) && (status === "all" || item.status === status) && item.name.includes(search.trim())), [sites, siteType, status, search]);
  const localSection = useMemo(() => selected && isGeologyFacility(selected.id) ? createLocalSectionModel({
    facilityId: selected.id,
    facilityName: selected.name,
    center: selected.wgs84,
  }) : null, [selected]);

  const selectSite = (id: string) => {
    setSelectedId(id);
    if (isGeologyFacility(id)) {
      setGeologyVisible(true);
      setIs3D(true);
    } else {
      setGeologyVisible(false);
    }
  };

  const toggleGeology = () => {
    if (!geologySupported) {
      selectSite("gujiaqiao-intake");
      return;
    }
    setGeologyVisible((value) => !value);
  };

  return <div className="app-shell overview-page"><AppHeader/><main className={`overview-layout ${geologyMode ? "geology-mode" : ""} ${geologyBackdrop ? "geology-backdrop" : ""}`}>
    <Suspense fallback={<div className="overview-map scene-loading-page">卫星工程底图加载中…</div>}><MapboxOverview sites={sites} zonesVisible={zonesVisible} siteType={siteType} status={status} search={search} selectedId={selectedId} geologyVisible={geologyVisible} geologyOpacity={geologyOpacity} geologyCutaway={geologyCutaway} geologyCutAxis={geologyCutAxis} geologyVerticalExaggeration={geologyVerticalExaggeration} geologyExploded={geologyExploded} onSelectSite={selectSite} onSelectZone={setZone} resetSignal={resetSignal} is3D={is3D}/></Suspense>

    {geologyMode && selected ? <aside className="geology-borehole-panel glass-panel">
      <div className="geology-contract-title"><span>BOREHOLE PROFILE</span><button onClick={() => setGeologyVisible(false)} title="返回工程检查器">‹</button></div>
      <h2>{selected.name}</h2>
      <p>{selected.wgs84[0].toFixed(5)}° E · {selected.wgs84[1].toFixed(5)}° N</p>
      {localSection && <div className="local-section-anchor"><span><b>真实地址锚点</b><small>EPSG:4326 · 设施档案坐标</small></span><em>{localSection.footprint.width} × {localSection.footprint.depth} m</em></div>}
      <button className={`depth-toggle ${showDepth ? "active" : ""}`} onClick={() => setShowDepth((value) => !value)}>{showDepth ? "隐藏深度" : "显示深度"}</button>
      <div className="borehole-profile">
        <div className="borehole-ruler"><span>0 m</span><span>5 m</span><span>10 m</span><span>15 m</span><span>19.5 m</span></div>
        <div className="borehole-column">{simulatedGeologyLayers.map((layer) => <i key={layer.id} style={{ flex: layer.bottomDepth - layer.topDepth, background: `#${layer.color.toString(16).padStart(6, "0")}` }}/>)}</div>
        <div className="borehole-layers">{simulatedGeologyLayers.map((layer) => <div key={layer.id} style={{ flex: layer.bottomDepth - layer.topDepth }}><i style={{ background: `#${layer.color.toString(16).padStart(6, "0")}` }}/><span><b>{layer.name}</b><small>{layer.description}</small>{showDepth && <em>顶 {layer.topDepth} m · 底 {layer.bottomDepth} m</em>}</span></div>)}</div>
      </div>
      <div className="borehole-site-switch"><button className={selectedId === "gujiaqiao-intake" ? "active" : ""} onClick={() => selectSite("gujiaqiao-intake")}>顾家桥</button><button className={selectedId === "liuxia-river-intake" ? "active" : ""} onClick={() => selectSite("liuxia-river-intake")}>留下河</button></div>
      <div className="borehole-readout">X={selected.wgs84[0].toFixed(5)} · Y={selected.wgs84[1].toFixed(5)}<br/>Azimuth 32° · Dip 90°</div>
    </aside> : <aside className="overview-inspector glass-panel">
      <div className="eyebrow">ENGINEERING INSPECTOR</div><h2>工程空间检查器</h2><p>卫星影像上的覆盖范围与排水关联分析</p>
      <label className="search-box"><Icon name="search"/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="搜索设施名称"/></label>
      <div className="filter-pair"><label><span>设施类型</span><select value={siteType} onChange={(event) => setSiteType(event.target.value)}><option value="all">全部类型</option><option value="intake">进水口</option><option value="pump-station">泵站</option><option value="landmark">区域地标</option></select></label><label><span>运行状态</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="all">全部状态</option><option value="normal">正常</option><option value="warning">预警</option><option value="offline">离线</option></select></label></div>
      <div className="layer-switch"><span><Icon name="layers"/><b>工程覆盖区</b><small>Mapbox GeoJSON 图层</small></span><button className={`toggle ${zonesVisible ? "on" : ""}`} onClick={() => setZonesVisible((value) => !value)}><i/></button></div>
      <div className="layer-switch geology-entry"><span><Icon name="cube"/><b>地下地质剖面</b><small>Three.js custom layer · 模拟数据</small></span><button className={`toggle ${geologyVisible && geologySupported ? "on" : ""}`} onClick={toggleGeology}><i/></button></div>
      <div className="section-title"><span>设施列表</span><small>{filtered.length} 个结果</small></div>
      <div className="overview-site-list">{filtered.map((site) => <button key={site.id} className={site.id === selectedId ? "selected" : ""} onClick={() => selectSite(site.id)}><i className={`status-${site.status}`}/><span><strong>{site.name}</strong><small>{site.wgs84[0].toFixed(5)}, {site.wgs84[1].toFixed(5)} · WGS84</small></span><StatusBadge status={site.status}/></button>)}</div>
      <div className="zone-detail"><div className="zone-color" style={{ background: zone.color }}/><span className="eyebrow">SELECTED AREA</span><h3>{zone.name}</h3><div><span><small>估算面积</small><b>{zone.area}</b></span><span><small>监测设备</small><b>{zone.deviceCount}</b></span><span><small>当前告警</small><b className={zone.alarmCount ? "orange" : "green"}>{zone.alarmCount}</b></span></div><p>{zone.note}</p></div>
    </aside>}

    {geologyMode && selected ? <aside className="geology-function-panel glass-panel">
      <div className="geology-panel-head"><div><span>GEOLOGICAL TOOLBOX</span><h2>地质剖面控制台</h2></div><button onClick={() => setGeologyVisible(false)}>×</button></div>
      <section className="contract-map-service"><h3>WEB MAP SERVICES / 地图服务</h3><label><i className="active"/><span><b>Mapbox Satellite Streets</b><small>当前工程底图</small></span></label><div><input value={wmsUrl} onChange={(event) => { setWmsUrl(event.target.value); setWmsNotice(""); }} placeholder="https://… / WMS URL"/><button onClick={() => setWmsNotice(wmsUrl.trim() ? "接口地址已登记；未主动请求或声明其为真实地质数据" : "请输入 WMS URL")}>登记</button></div>{wmsNotice && <p>{wmsNotice}</p>}</section>
      <section><h3>VIEW / 视图</h3><div className="contract-button-row"><button className={is3D ? "active" : ""} onClick={() => setIs3D(true)}>三维相机</button><button className={!is3D ? "active" : ""} onClick={() => setIs3D(false)}>俯视相机</button></div><button className={`contract-wide-button ${geologyBackdrop ? "active" : ""}`} onClick={() => setGeologyBackdrop((value) => !value)}>背景增强 {geologyBackdrop ? "ON" : "OFF"}</button></section>
      <section><h3>OPACITY / 透明度</h3><label className="contract-range"><input type="range" min="20" max="92" value={Math.round(geologyOpacity * 100)} onChange={(event) => setGeologyOpacity(Number(event.target.value) / 100)}/><b>{Math.round(geologyOpacity * 100)}%</b></label></section>
      <section><h3>VERTICAL EXAGGERATION / 垂向夸张</h3><label className="contract-range"><input type="range" min="1" max="10" step="0.2" value={geologyVerticalExaggeration} onChange={(event) => setGeologyVerticalExaggeration(Number(event.target.value))}/><b>{geologyVerticalExaggeration.toFixed(1)}×</b></label></section>
      <section><h3>CROSS SECTIONS / 剖切方向</h3><div className="cut-axis-grid">{cutAxisLabels.map((item) => <button key={item.axis} className={geologyCutAxis === item.axis ? "active" : ""} onClick={() => setGeologyCutAxis(item.axis)}>{item.label}</button>)}</div><label className="contract-range"><input type="range" min="0" max="80" value={geologyCutaway} onChange={(event) => setGeologyCutaway(Number(event.target.value))}/><b>{geologyCutaway}%</b></label></section>
      <section><h3>EXPLODING OBJECTS / 地层分解</h3><div className="contract-button-row"><button className={geologyExploded ? "active" : ""} onClick={() => setGeologyExploded(true)}>爆炸地层</button><button onClick={() => { setGeologyExploded(false); setGeologyCutaway(38); setGeologyVerticalExaggeration(2.4); setGeologyCutAxis("y"); }}>复位对象</button></div></section>
      <section className="contract-location"><h3>BOREHOLE / 钻孔定位</h3><div><span>{selected.wgs84[0].toFixed(5)}° E</span><span>{selected.wgs84[1].toFixed(5)}° N</span><button onClick={() => selectSite(selectedId === "gujiaqiao-intake" ? "liuxia-river-intake" : "gujiaqiao-intake")}>切换</button></div></section>
      {localSection && <section className="section-data-boundary"><h3>DATA BOUNDARY / 数据接入边界</h3><div className="section-source-list">{localSection.dataSources.map((source) => <div key={source.id} className={source.status}><i/><span><b>{source.label}</b><small>{source.note}</small>{source.endpoint && <code>{source.endpoint}</code>}</span><em>{source.status === "available" ? "已定位" : "待接入"}</em></div>)}</div><div className="section-relation-legend"><span className="surface">地表预览</span><span className="pipe">模拟管线</span><span className="soil">模拟土层</span></div></section>}
      <p className="contract-warning">SIMULATED GEOLOGY · 教学模拟地层，不代表真实钻孔或工程勘察结论。</p>
    </aside> : selected && <aside className="overview-detail glass-panel"><div className="section-title"><span>重点设施</span><StatusBadge status={selected.status}/></div><h3>{selected.name}</h3><p>{selected.description}</p><div className="mini-metrics"><span><small>液位</small><b>{selected.metrics.waterLevel} m</b></span><span><small>流量</small><b>{selected.metrics.instantFlow} m³/s</b></span><span><small>功率</small><b>{selected.metrics.power} kW</b></span></div><div className="coordinate-note">地图坐标已由 GCJ-02 转换为 WGS84</div><Link to={`/facility/${selected.id}`}><Icon name="cube"/>进入设备三维场景</Link></aside>}

    {geologyMode && <><div className="geology-axis-overlay"><span className="axis-n">N</span><span className="axis-x">X / E</span><span className="axis-y">Y / N</span><span className="axis-z">Z / DEPTH</span></div><div className="geology-model-caption"><b>{selected?.name} · REAL-ADDRESS LOCAL SECTION</b><span>真实坐标定位 · 程序化地表 / 模拟管线 / 四层模拟地质体</span></div></>}
    {!geologyMode && <div className="overview-tools"><button onClick={() => setResetSignal((value) => value + 1)} title="恢复初始视角"><Icon name="reset"/></button><button className={is3D ? "active" : ""} onClick={() => setIs3D((value) => !value)}>{is3D ? "3D" : "2D"}</button><button className={zonesVisible ? "active" : ""} onClick={() => setZonesVisible((value) => !value)} title="覆盖区图层"><Icon name="layers"/></button><Link to="/" title="返回城市监控"><Icon name="map"/></Link></div>}
    <div className="overview-caption"><span>地图要素 · Mapbox GL JS</span><em>地下模拟体 · Three.js custom layer</em></div>
  </main></div>;
}
