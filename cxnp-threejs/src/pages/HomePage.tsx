import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AppHeader } from "../components/AppHeader";
import { Icon } from "../components/Icon";
import { StatusBadge } from "../components/StatusBadge";
import { TrendChart } from "../components/TrendChart";
import { CityMap } from "../features/city-map/CityMap";
import { usePlatform } from "../state/PlatformContext";
import type { FacilityStatus, FacilityType } from "../types/domain";

const typeLabels: Record<FacilityType | "all", string> = { all: "全部设施", intake: "进水口", "pump-station": "泵站", landmark: "地标" };

export default function HomePage() {
  const { sites, selectedId, setSelectedId, updatePump, updateGate, loading } = usePlatform();
  const navigate = useNavigate();
  const [typeFilter, setTypeFilter] = useState<FacilityType | "all">("all");
  const [statusFilter, setStatusFilter] = useState<FacilityStatus | "all">("all");
  const selected = sites.find((site) => site.id === selectedId) ?? sites[0];
  const visibleSites = useMemo(() => sites.filter((site) => (typeFilter === "all" || site.type === typeFilter) && (statusFilter === "all" || site.status === statusFilter)), [sites, typeFilter, statusFilter]);
  const alarms = sites.flatMap((site) => site.alarms.map((alarm) => ({ ...alarm, siteName: site.name })));

  return <div className="app-shell city-page">
    <AppHeader />
    <main className="city-layout">
      <aside className="side-panel left-panel panel-scroll">
        <section className="panel-section intro-block"><div className="eyebrow">OPERATION OVERVIEW</div><h2>城市排水态势</h2><p>区域设施运行、告警与空间位置统一监控</p></section>
        <section className="stats-grid">
          <button onClick={() => setStatusFilter("all")} className={statusFilter === "all" ? "active" : ""}><strong>{sites.length}</strong><span>设施总数</span></button>
          <button onClick={() => setStatusFilter("normal")} className={statusFilter === "normal" ? "active" : ""}><strong className="green">{sites.filter((s) => s.status === "normal").length}</strong><span>运行正常</span></button>
          <button onClick={() => setStatusFilter("warning")} className={statusFilter === "warning" ? "active" : ""}><strong className="orange">{sites.filter((s) => s.status === "warning").length}</strong><span>当前预警</span></button>
          <button onClick={() => setStatusFilter("offline")} className={statusFilter === "offline" ? "active" : ""}><strong className="muted">{sites.filter((s) => s.status === "offline").length}</strong><span>通讯离线</span></button>
        </section>
        <section className="panel-section">
          <div className="section-title"><span>设施筛选</span><small>{visibleSites.length} / {sites.length}</small></div>
          <div className="segmented-control" aria-label="设施类型筛选">
            {(Object.keys(typeLabels) as Array<FacilityType | "all">).map((type) => <button key={type} className={typeFilter === type ? "active" : ""} onClick={() => setTypeFilter(type)}>{typeLabels[type]}</button>)}
          </div>
          <div className="facility-list">
            {visibleSites.map((site) => <button key={site.id} className={site.id === selectedId ? "selected" : ""} onClick={() => setSelectedId(site.id)}>
              <span className={`facility-icon status-${site.status}`}><Icon name={site.type === "pump-station" ? "pump" : "activity"}/></span>
              <span><strong>{site.name}</strong><small>{site.description}</small></span>
              <StatusBadge status={site.status}/>
            </button>)}
          </div>
        </section>
        <section className="panel-section alarm-section">
          <div className="section-title"><span>实时告警</span><small className="orange">{alarms.length} 条</small></div>
          {alarms.map((alarm) => <button key={alarm.id} onClick={() => setSelectedId(sites.find((s) => s.name === alarm.siteName)?.id ?? selectedId)} className={`alarm-row alarm-${alarm.level}`}>
            <Icon name="alert"/><span><strong>{alarm.text}</strong><small>{alarm.siteName} · {alarm.time}</small></span>
          </button>)}
        </section>
      </aside>

      <section className="map-column">
        <div className="map-heading"><div><span className="breadcrumb">城市空间 / 设施监控</span><strong>西湖区 · 城西南排水系统</strong></div><span className="map-coordinate">GCJ-02 · 120.056338, 30.235655</span></div>
        <CityMap sites={visibleSites} selectedId={selectedId} onSelect={(id) => { setSelectedId(id); navigate(`/facility/${id}`); }}/>
        <div className="map-legend"><span><i className="normal"/>正常</span><span><i className="warning"/>预警</span><span><i className="offline"/>离线</span><em>城市底图：高德地图</em></div>
      </section>

      <aside className="side-panel right-panel panel-scroll">
        {loading || !selected ? <div className="panel-loading">监测数据加载中…</div> : <>
          <section className="selected-heading"><div><span className="eyebrow">FACILITY DETAIL</span><h2>{selected.name}</h2></div><StatusBadge status={selected.status} pulse={selected.status === "normal"}/><p>{selected.description}</p>{selected.isMock && <span className="demo-label">演示点位 · 非工程测绘坐标</span>}</section>
          <section className="primary-metrics">
            <div><span>实时液位</span><strong>{selected.metrics.waterLevel.toFixed(2)}<small>m</small></strong></div>
            <div><span>瞬时流量</span><strong>{selected.metrics.instantFlow.toFixed(2)}<small>m³/s</small></strong></div>
          </section>
          <section className="metric-grid">
            <div><span>累计排水量</span><strong>{(selected.metrics.totalDrainage / 10000).toFixed(2)} 万 m³</strong></div>
            <div><span>实时功率</span><strong>{selected.metrics.power.toFixed(1)} kW</strong></div>
            <div><span>运行泵机</span><strong>{selected.metrics.runningPumps} 台</strong></div>
            <div><span>闸门开度</span><strong>{selected.metrics.gateOpening}%</strong></div>
          </section>
          <section className="control-card">
            <div className="section-title"><span>设备模拟控制</span><small>同步至三维场景</small></div>
            <div className="control-row"><span><Icon name="pump"/>泵机状态</span><button className={`toggle ${selected.metrics.pumpRunning ? "on" : ""}`} onClick={() => updatePump(selected.id, !selected.metrics.pumpRunning)} aria-pressed={selected.metrics.pumpRunning}><i/><b>{selected.metrics.pumpRunning ? "运行" : "停止"}</b></button></div>
            <label className="range-control"><span><Icon name="gate"/>闸门开度 <b>{selected.metrics.gateOpening}%</b></span><input type="range" min="0" max="100" value={selected.metrics.gateOpening} onChange={(event) => updateGate(selected.id, Number(event.target.value))}/></label>
          </section>
          <section className="trend-card"><div className="section-title"><span>近 24 小时流量</span><small>m³/s</small></div><TrendChart data={selected.trend24h} metric="flow" compact/></section>
          <div className={`condition-bar alarm-${selected.metrics.alarmLevel}`}><Icon name="activity"/><span><small>运行诊断</small><strong>{selected.metrics.alarmText}</strong></span></div>
          <Link className="primary-action" to={`/facility/${selected.id}`}><Icon name="cube"/><span>查看三维设施<small>进入设备级数字孪生场景</small></span><b>→</b></Link>
          <div className="updated-at">数据更新：{new Date(selected.updatedAt).toLocaleString("zh-CN", { hour12: false })}</div>
        </>}
      </aside>
    </main>
  </div>;
}
