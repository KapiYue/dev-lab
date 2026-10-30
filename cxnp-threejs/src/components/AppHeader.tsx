import { Link, useLocation } from "react-router-dom";
import { Clock } from "./Clock";
import { Icon } from "./Icon";

export function AppHeader() {
  const location = useLocation();
  return <header className="app-header">
    <div className="brand-mark"><span/><i/></div>
    <div className="brand-copy"><small>CXNP WATER DIGITAL TWIN</small><h1>城西南排水数字孪生监控平台</h1></div>
    <nav aria-label="主导航">
      <Link className={location.pathname === "/" ? "active" : ""} to="/"><Icon name="map"/>城市监控</Link>
      <Link className={location.pathname === "/overview" ? "active" : ""} to="/overview"><Icon name="layers"/>工程概览</Link>
    </nav>
    <div className="header-status"><span className="live-dot"/>系统在线</div>
    <Clock />
  </header>;
}
