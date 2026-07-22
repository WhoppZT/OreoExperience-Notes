import { NavLink } from 'react-router-dom'
import { StickyNote, Globe, Settings } from 'lucide-react'
import clsx from 'clsx'

const tabs = [
  { to: '/', icon: StickyNote, label: 'Notas' },
  { to: '/servicio', icon: Globe, label: 'Servicio' },
  { to: '/ajustes', icon: Settings, label: 'Ajustes' },
] as const

export function BottomNav() {
  return (
    <nav className="fixed bottom-0 inset-x-0 z-50 bg-nav-bg border-t border-nav-border">
      <div className="max-w-lg mx-auto flex items-center justify-around py-1.5 px-4">
        {tabs.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) => clsx(
              'flex flex-col items-center gap-0.5 px-4 py-1 rounded-xl transition-all duration-200',
              isActive
                ? 'text-accent'
                : 'text-on-surface-faint hover:text-on-surface-muted',
            )}
          >
            {({ isActive }) => (
              <>
                <div className={clsx(
                  'w-10 h-10 flex items-center justify-center rounded-xl transition-all duration-200',
                  isActive
                    ? 'bg-accent/20 border border-accent/40'
                    : '',
                )}>
                  <Icon size={20} />
                </div>
                <span className={clsx(
                  'text-[10px] font-medium',
                  isActive ? 'font-bold' : '',
                )}>
                  {label}
                </span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
