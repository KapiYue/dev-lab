import type { TrendPoint } from "../types/domain";

export function TrendChart({ data, metric = "flow", compact = false }: { data: TrendPoint[]; metric?: "flow" | "level" | "power"; compact?: boolean }) {
  if (!data.length) return <div className="chart-empty">暂无趋势数据</div>;
  const values = data.map((item) => item[metric]);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const width = 480;
  const height = compact ? 92 : 150;
  const pad = 10;
  const points = values.map((value, index) => {
    const x = pad + (index / Math.max(1, values.length - 1)) * (width - pad * 2);
    const y = height - pad - ((value - min) / Math.max(0.01, max - min)) * (height - pad * 2);
    return `${x},${y}`;
  }).join(" ");
  const area = `${pad},${height - pad} ${points} ${width - pad},${height - pad}`;
  return <div className="trend-chart">
    <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label={`${metric} 趋势图`} preserveAspectRatio="none">
      <defs><linearGradient id={`area-${metric}`} x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="var(--cyan)" stopOpacity=".34"/><stop offset="1" stopColor="var(--cyan)" stopOpacity="0"/></linearGradient></defs>
      {[.25,.5,.75].map((ratio) => <line key={ratio} x1="0" x2={width} y1={height * ratio} y2={height * ratio} className="grid-line"/>)}
      <polygon points={area} fill={`url(#area-${metric})`}/>
      <polyline points={points} className="chart-line"/>
    </svg>
    {!compact && <div className="chart-labels"><span>{data[0].label}</span><span>{data[Math.floor(data.length / 2)].label}</span><span>{data.at(-1)?.label}</span></div>}
  </div>;
}
