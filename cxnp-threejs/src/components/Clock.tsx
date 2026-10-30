import { useEffect, useState } from "react";

export function Clock({ compact = false }: { compact?: boolean }) {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timer);
  }, []);
  return <time className="clock">
    {!compact && <span>{new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", weekday: "short" }).format(now)}</span>}
    <strong>{now.toLocaleTimeString("zh-CN", { hour12: false })}</strong>
  </time>;
}
