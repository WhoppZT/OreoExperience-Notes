import { Outlet, useLocation } from 'react-router-dom'
import { BottomNav } from './BottomNav'

const HIDE_NAV_PATHS = ['/editor']

export function Layout() {
  const { pathname } = useLocation()
  const hideNav = HIDE_NAV_PATHS.some(p => pathname.startsWith(p))

  return (
    <div className="flex flex-col min-h-dvh bg-bg">
      <main className="flex-1 pb-20">
        <Outlet />
      </main>
      {!hideNav && <BottomNav />}
    </div>
  )
}
