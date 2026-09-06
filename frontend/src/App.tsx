import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { AppShell } from './components/AppShell'
import { ProtectedRoute } from './components/ProtectedRoute'
import { BriefPage } from './pages/BriefPage'
import { DashboardPage } from './pages/DashboardPage'
import { ImportsPage, NotFoundPage, ReportsPage } from './pages/DeferredPages'
import { LoginPage } from './pages/LoginPage'
import { OrdersPage } from './pages/OrdersPage'
import { ProductsPage } from './pages/ProductsPage'
import { PurchaseOrdersPage } from './pages/PurchaseOrdersPage'
import { RisksPage } from './pages/RisksPage'
import { SuppliersPage } from './pages/SuppliersPage'

export default function App() {
  return <BrowserRouter><AuthProvider><Routes><Route path="/login" element={<LoginPage />} /><Route element={<ProtectedRoute />}><Route element={<AppShell />}><Route index element={<DashboardPage />} /><Route path="products" element={<ProductsPage />} /><Route path="suppliers" element={<SuppliersPage />} /><Route path="orders" element={<OrdersPage />} /><Route path="purchase-orders" element={<PurchaseOrdersPage />} /><Route path="risks" element={<RisksPage />} /><Route path="brief" element={<BriefPage />} /><Route path="reports" element={<ReportsPage />} /><Route path="imports" element={<ImportsPage />} /><Route path="*" element={<NotFoundPage />} /></Route></Route></Routes></AuthProvider></BrowserRouter>
}
