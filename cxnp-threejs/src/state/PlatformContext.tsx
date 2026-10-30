import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { siteService } from "../services/siteService";
import type { DrainageFacility, FacilityStatus } from "../types/domain";

interface PlatformState {
  sites: DrainageFacility[];
  selectedId: string;
  setSelectedId: (id: string) => void;
  updatePump: (id: string, running: boolean) => void;
  updateGate: (id: string, opening: number) => void;
  loading: boolean;
}

const PlatformContext = createContext<PlatformState | null>(null);

export function PlatformProvider({ children }: { children: ReactNode }) {
  const [sites, setSites] = useState<DrainageFacility[]>([]);
  const [selectedId, setSelectedId] = useState(() => sessionStorage.getItem("cxnp-selected-site") ?? "southwest-pump");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    siteService.list().then(setSites).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    sessionStorage.setItem("cxnp-selected-site", selectedId);
  }, [selectedId]);

  const patchSite = (id: string, patch: (site: DrainageFacility) => DrainageFacility) => {
    setSites((current) => current.map((site) => site.id === id ? patch(site) : site));
  };

  const updatePump = (id: string, running: boolean) => patchSite(id, (site) => ({
    ...site,
    status: (site.status === "offline" ? "offline" : running ? "normal" : "warning") as FacilityStatus,
    metrics: {
      ...site.metrics,
      pumpRunning: running,
      runningPumps: running ? Math.max(1, site.metrics.runningPumps) : 0,
      instantFlow: running ? Math.max(1.2, site.metrics.instantFlow) : 0,
      power: running ? Math.max(46, site.metrics.power) : 3.2,
      alarmLevel: running ? "normal" : "warning",
      alarmText: running ? "设备已切换至运行状态" : "泵机已手动停止",
    },
  }));

  const updateGate = (id: string, opening: number) => patchSite(id, (site) => ({
    ...site,
    metrics: {
      ...site.metrics,
      gateOpening: opening,
      instantFlow: site.metrics.pumpRunning ? Number((Math.max(0.2, opening / 20) * 0.92).toFixed(2)) : 0,
    },
  }));

  const value = useMemo(() => ({ sites, selectedId, setSelectedId, updatePump, updateGate, loading }), [sites, selectedId, loading]);
  return <PlatformContext.Provider value={value}>{children}</PlatformContext.Provider>;
}

export function usePlatform() {
  const value = useContext(PlatformContext);
  if (!value) throw new Error("usePlatform must be used inside PlatformProvider");
  return value;
}
