import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { useNotesStore } from '@/stores/notesStore'
import { useServicioStore } from '@/stores/servicioStore'
import { observeNotes, observeTrashedNotes, observeRecords } from '@/lib/firestore'
import { Layout } from '@/components/Layout'
import { LoginScreen } from '@/screens/LoginScreen'
import { HomeScreen } from '@/screens/HomeScreen'
import { EditorScreen } from '@/screens/EditorScreen'
import { ServicioScreen } from '@/screens/ServicioScreen'
import { SettingsScreen } from '@/screens/SettingsScreen'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuthStore()
  if (loading) {
    return (
      <div className="min-h-dvh flex items-center justify-center bg-bg">
        <div className="w-8 h-8 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function AppContent() {
  const user = useAuthStore(s => s.user)
  const setNotes = useNotesStore(s => s.setNotes)
  const setTrashed = useNotesStore(s => s.setTrashed)
  const setRecords = useServicioStore(s => s.setRecords)

  useEffect(() => {
    if (!user) return
    const unsubs = [
      observeNotes(user.uid, setNotes),
      observeTrashedNotes(user.uid, setTrashed),
      observeRecords(user.uid, setRecords),
    ]
    return () => unsubs.forEach(u => u())
  }, [user?.uid])

  return (
    <Routes>
      <Route path="/login" element={<LoginScreen />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<HomeScreen />} />
        <Route path="/editor/:id" element={<EditorScreen />} />
        <Route path="/servicio" element={<ServicioScreen />} />
        <Route path="/ajustes" element={<SettingsScreen />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  const init = useAuthStore(s => s.init)

  useEffect(() => {
    const unsub = init()
    return unsub
  }, [])

  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  )
}
