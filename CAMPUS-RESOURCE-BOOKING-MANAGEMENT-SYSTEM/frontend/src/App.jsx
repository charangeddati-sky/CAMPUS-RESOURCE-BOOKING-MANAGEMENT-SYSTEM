import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import AppShell from './components/layout/AppShell'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import Campus from './pages/Campus'
import Building from './pages/Building'
import Floor from './pages/Floor'
import Resource from './pages/Resource'
import Book from './pages/Book'
import Bookings from './pages/Bookings'
import BookingDetail from './pages/BookingDetail'
import Confirmation from './pages/Confirmation'
import Dashboard from './pages/Dashboard'
import Approvals from './pages/Approvals'
import AiBooking from './pages/AiBooking'
import AiChat from './pages/AiChat'
import AvailableNow from './pages/AvailableNow'
import SearchPage from './pages/SearchPage'
import Favorites from './pages/Favorites'
import Notifications from './pages/Notifications'
import CalendarPage from './pages/CalendarPage'
import Events from './pages/Events'
import Exams from './pages/Exams'
import Issues from './pages/Issues'
import AdminHome from './pages/admin/AdminHome'
import FloorEditorPage from './pages/admin/FloorEditorPage'
import AnalyticsDashboard from './pages/admin/AnalyticsDashboard'
import ResourceManager from './pages/admin/ResourceManager'

function Guard({ children, roles }) {
  const { isAuthenticated, role } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (roles && !roles.includes(role)) return <Navigate to="/dashboard" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        element={(
          <Guard>
            <AppShell />
          </Guard>
        )}
      >
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/campus" element={<Campus />} />
        <Route path="/buildings/:id" element={<Building />} />
        <Route path="/floors/:id" element={<Floor />} />
        <Route path="/resources/:id" element={<Resource />} />
        <Route path="/book/:resourceId?" element={<Book />} />
        <Route path="/bookings" element={<Bookings />} />
        <Route path="/bookings/:id" element={<BookingDetail />} />
        <Route path="/bookings/:id/confirmation" element={<Confirmation />} />
        <Route path="/approvals" element={<Guard roles={['PROFESSOR', 'ADMIN']}><Approvals /></Guard>} />
        <Route path="/ai-booking" element={<AiBooking />} />
        <Route path="/ai-chat" element={<AiChat />} />
        <Route path="/available-now" element={<AvailableNow />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/favorites" element={<Favorites />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/calendar" element={<CalendarPage />} />
        <Route path="/events" element={<Events />} />
        <Route path="/exams" element={<Exams />} />
        <Route path="/issues" element={<Issues />} />
        <Route path="/admin" element={<Guard roles={['ADMIN']}><AdminHome /></Guard>} />
        <Route path="/admin/analytics" element={<Guard roles={['ADMIN']}><AnalyticsDashboard /></Guard>} />
        <Route path="/admin/resources" element={<Guard roles={['ADMIN']}><ResourceManager /></Guard>} />
        <Route path="/admin/floors/:id/editor" element={<Guard roles={['ADMIN']}><FloorEditorPage /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
