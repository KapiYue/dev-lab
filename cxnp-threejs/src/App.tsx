import { lazy, Suspense } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { PlatformProvider } from "./state/PlatformContext";

const HomePage = lazy(() => import("./pages/HomePage"));
const OverviewPage = lazy(() => import("./pages/OverviewPage"));
const FacilityScenePage = lazy(() => import("./pages/FacilityScenePage"));

export default function App() {
  return <BrowserRouter><PlatformProvider><Suspense fallback={<div className="route-loader"><span/><strong>数字孪生平台加载中</strong></div>}><Routes><Route path="/" element={<HomePage/>}/><Route path="/overview" element={<OverviewPage/>}/><Route path="/facility/:id" element={<FacilityScenePage/>}/><Route path="*" element={<HomePage/>}/></Routes></Suspense></PlatformProvider></BrowserRouter>;
}
