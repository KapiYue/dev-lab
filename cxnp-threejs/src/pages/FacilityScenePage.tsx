import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Clock } from "../components/Clock";
import { Icon } from "../components/Icon";
import { StatusBadge } from "../components/StatusBadge";
import { TrendChart } from "../components/TrendChart";
import { usePlatform } from "../state/PlatformContext";
import type { FacilitySceneView } from "../features/facility-scene/FacilityTwinScene";
import { mapMonitoringToPipeFlow } from "../features/facility-scene/monitoringPipeFlow";

const FacilityTwinScene = lazy(() => import("../features/facility-scene/FacilityTwinScene").then((module) => ({ default: module.FacilityTwinScene })));

export default function FacilityScenePage() {
  const { id = "southwest-pump" } = useParams();
  const navigate = useNavigate();
  const { sites, setSelectedId, updatePump, updateGate } = usePlatform();
  const facility = sites.find((site) => site.id === id) ?? sites.find((site) => site.id === "southwest-pump");

  const [showHotspots, setShowHotspots] = useState(true);// 是显示热点标记
  const [showOrdinaryPipe, setShowOrdinaryPipe] = useState(true);// 是显示普通管道
  const [showInstancedPipes, setShowInstancedPipes] = useState(true);// 是显示实例化管道
  const [showCurvedPipe, setShowCurvedPipe] = useState(true);// 弯曲管道
  const [showShaderFlow, setShowShaderFlow] = useState(true);// 水流效果，通过 ShaderMaterial / 自定义 Shader 实现管道内部的动态流动视觉效果
  const [showParticleFlow, setShowParticleFlow] = useState(true);// 粒子水流效果，使用 Points、粒子位置更新
  const [viewMode, setViewMode] = useState<FacilitySceneView>("study");
  const [manholeOpen, setManholeOpen] = useState(false);
  const [resetSignal, setResetSignal] = useState(0);
  const [activeDevice, setActiveDevice] = useState("1# 排水泵");
  useEffect(() => {
    setActiveDevice(facility?.type === "intake" ? "进水提升泵组" : facility?.type === "landmark" ? "应急排涝泵" : "1# 排水泵");
    setManholeOpen(false);
  }, [facility?.id, facility?.type]);
  const tasks = useMemo(() => [
    { name: "日常排水调度", value: facility?.metrics.planDrainage ?? 0, unit: "m³", progress: 72 },
    { name: "设备巡检任务", value: 7, unit: "/ 9", progress: 78 },
    { name: "闸门联动策略", value: facility?.metrics.gateOpening ?? 0, unit: "%", progress: facility?.metrics.gateOpening ?? 0 },
  ], [facility]);
  const monitoringFlow = useMemo(() => facility ? mapMonitoringToPipeFlow({
    instantFlow: facility.metrics.instantFlow,
    pumpRunning: facility.metrics.pumpRunning,
    facilityStatus: facility.status,
    alarmLevel: facility.metrics.alarmLevel,
  }) : null, [facility]);

  if (!facility || !monitoringFlow) return <div className="scene-loading-page">设施数据加载中…</div>;
  const returnCity = () => { setSelectedId(facility.id); navigate("/"); };

  return <div className="facility-scene-page">
    <Suspense fallback={<div className="scene-loading-page">三维场景初始化中…</div>}><FacilityTwinScene facilityId={facility.id} running={facility.metrics.pumpRunning} gateOpening={facility.metrics.gateOpening} manholeOpen={manholeOpen} showHotspots={showHotspots} showOrdinaryPipe={showOrdinaryPipe} showInstancedPipes={showInstancedPipes} showCurvedPipe={showCurvedPipe} showShaderFlow={showShaderFlow} showParticleFlow={showParticleFlow} shaderFlowStatus={monitoringFlow.status} shaderFlowSpeed={monitoringFlow.speed} viewMode={viewMode} resetSignal={resetSignal} onSelect={(name) => { setActiveDevice(name); if (name.startsWith("检修井盖")) setManholeOpen((value) => !value); }}/></Suspense>
    <header className="scene-header"><button onClick={returnCity} className="icon-button"><Icon name="back"/></button><div><span>DIGITAL TWIN FACILITY</span><h1>{facility.name}</h1></div><StatusBadge status={facility.status} pulse/><nav><button onClick={returnCity}>返回城市监控</button><Link to="/overview">工程概览</Link></nav><Clock/></header>
    <aside className="scene-panel scene-left">
      <div className="scene-panel-title"><span>实时运行数据</span><i>LIVE</i></div>
      <div className="scene-kpi highlight"><span>计划排水量</span><strong>{facility.metrics.planDrainage.toLocaleString()}<small> m³</small></strong><em>今日目标完成 72%</em></div>
      <div className="scene-kpi-grid"><div><span>开机台数</span><strong>{facility.metrics.runningPumps}<small> 台</small></strong></div><div><span>实时液位</span><strong>{facility.metrics.waterLevel}<small> m</small></strong></div><div><span>瞬时流量</span><strong>{facility.metrics.instantFlow}<small> m³/s</small></strong></div><div><span>实时功率</span><strong>{facility.metrics.power}<small> kW</small></strong></div></div>
      <div className="scene-data-row"><span>日电量</span><strong>{facility.metrics.dailyPower.toLocaleString()} kWh</strong></div><div className="scene-data-row"><span>预估电费</span><strong>¥ {facility.metrics.dailyCost.toLocaleString()}</strong></div>
      <div className={`scene-alarm alarm-${facility.metrics.alarmLevel}`}><Icon name="alert"/><span><small>运行诊断</small><strong>{facility.metrics.alarmText}</strong></span></div>
      <div className="scene-learning-card">
        <div className="scene-panel-title compact"><span>管道学习阶段</span><small>阶段 7 / 9</small></div>
        <p>{viewMode === "study" ? "当前监测流量、泵机启停和告警等级正实时驱动 Shader 与路径粒子。" : "原设备场景视角，不显示教学对象。"}</p>
        <div className="scene-view-switch" role="group" aria-label="场景视角">
          <button className={viewMode === "normal" ? "active" : ""} onClick={() => setViewMode("normal")}>正常视角</button>
          <button className={viewMode === "study" ? "active" : ""} onClick={() => setViewMode("study")}>教学视角</button>
        </div>
        <button className={`learning-option ${showOrdinaryPipe ? "active" : ""}`} onClick={() => setShowOrdinaryPipe((value) => !value)} aria-pressed={showOrdinaryPipe}>
          <span><i/>普通 Mesh · 单根</span><b>{showOrdinaryPipe ? "已显示" : "已隐藏"}</b>
        </button>
        <button className={`learning-option ${showInstancedPipes ? "active" : ""}`} onClick={() => setShowInstancedPipes((value) => !value)} aria-pressed={showInstancedPipes}>
          <span><i/>InstancedMesh · 实例状态</span><b>{showInstancedPipes ? "已显示" : "已隐藏"}</b>
        </button>
        <button className={`learning-option curved ${showCurvedPipe ? "active" : ""}`} onClick={() => setShowCurvedPipe((value) => !value)} aria-pressed={showCurvedPipe}>
          <span><i/>TubeGeometry · 弯曲管道</span><b>{showCurvedPipe ? "已显示" : "已隐藏"}</b>
        </button>
        <button className={`learning-option shader ${showShaderFlow ? "active" : ""}`} onClick={() => setShowShaderFlow((value) => !value)} aria-pressed={showShaderFlow}>
          <span><i/>Shader · 水流与发光</span><b>{showShaderFlow ? "已启用" : "静态细流线"}</b>
        </button>
        <button className={`learning-option particles ${showParticleFlow ? "active" : ""}`} onClick={() => setShowParticleFlow((value) => !value)} aria-pressed={showParticleFlow}>
          <span><i/>Points · 路径粒子水流</span><b>{showParticleFlow ? "已启用" : "已关闭"}</b>
        </button>
        <div className={`shader-flow-controls monitoring-${monitoringFlow.status}`} aria-label="监测数据到水流效果的实时映射">
          <div className="monitoring-flow-state">
            <span><i/>监测映射</span><b>{monitoringFlow.status === "normal" ? "正常 · 蓝色" : monitoringFlow.status === "warning" ? "预警 · 橙色" : monitoringFlow.status === "critical" ? "严重 · 红色" : "离线 · 灰色"}</b>
          </div>
          <div className={`shader-speed-toggle ${monitoringFlow.flowing ? "active" : ""}`}>
            <span>{monitoringFlow.flowing ? "Shader + 粒子流动" : facility.status === "offline" ? "离线 · 粒子隐藏" : "停止 · 粒子隐藏"}</span><b>速度 {monitoringFlow.speed.toFixed(2)}</b>
          </div>
          <small>instantFlow {facility.metrics.instantFlow.toFixed(2)} m³/s × 0.20 · pumpRunning {facility.metrics.pumpRunning ? "true" : "false"}</small>
        </div>
        <div className="pipe-status-guide" aria-label="管道实例状态图例">
          <span className="normal"><i/>正常</span><span className="warning"><i/>预警</span>
          <span className="critical"><i/>异常</span><span className="stopped"><i/>停止</span>
        </div>
      </div>
      <div className="scene-panel-title compact"><span>设备控制</span><small>{activeDevice}</small></div>
      <div className="control-row"><span><Icon name="pump"/>水泵</span><button className={`toggle ${facility.metrics.pumpRunning ? "on" : ""}`} onClick={() => updatePump(facility.id, !facility.metrics.pumpRunning)}><i/><b>{facility.metrics.pumpRunning ? "运行" : "停止"}</b></button></div>
      <label className="range-control"><span><Icon name="gate"/>闸门开度 <b>{facility.metrics.gateOpening}%</b></span><input type="range" min="0" max="100" value={facility.metrics.gateOpening} onChange={(event) => updateGate(facility.id, Number(event.target.value))}/></label>
    </aside>
    <aside className="scene-panel scene-right">
      <div className="scene-panel-title"><span>运行任务</span><small>自动调度中</small></div>
      <div className="task-list">{tasks.map((task) => <div key={task.name}><span><b>{task.name}</b><em>{task.value.toLocaleString()} {task.unit}</em></span><i><b style={{ width: `${task.progress}%` }}/></i></div>)}</div>
      <div className="scene-panel-title compact"><span>近 7 日排水趋势</span><small>m³/s</small></div><TrendChart data={facility.trend7d} metric="flow" compact/>
      <div className="scene-panel-title compact"><span>视频监控</span><small>CAM 03</small></div>
      <button className="video-mock"><span className="camera-grid"/><i>● LIVE</i><strong>进水闸前池</strong><small>演示监控画面</small></button>
    </aside>
    <div className="scene-toolbar"><button className={showHotspots ? "active" : ""} onClick={() => setShowHotspots((value) => !value)}><Icon name="eye"/><span>设备热点</span></button><button onClick={() => setResetSignal((value) => value + 1)}><Icon name="reset"/><span>重置视角</span></button><button className={manholeOpen ? "active" : ""} onClick={() => { setManholeOpen((value) => !value); setActiveDevice("检修井盖"); }}><Icon name="cube"/><span>{manholeOpen ? "关闭井盖" : "剖视检验"}</span></button></div>
    <div className="scene-legend"><span><i className="normal"/>运行设备</span><span><i className="warning"/>井盖与管网</span><em>拖动旋转 · 滚轮缩放 · 右键平移</em></div>
  </div>;
}
