import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Palette, Database, Trash2, FileText, Sparkles, ExternalLink, LogOut } from 'lucide-react'
import { useAuthStore } from '@/stores/authStore'
import clsx from 'clsx'

function SettingsSection({ title }: { title: string }) {
  return (
    <div className="flex items-center gap-2 px-5 pt-5 pb-2">
      <div className="w-4 h-0.5 bg-accent/80 rounded-full" />
      <h2 className="text-[11px] font-semibold tracking-wider text-on-surface-muted uppercase">{title}</h2>
    </div>
  )
}

function SettingsRow({ icon: Icon, label, trailing, onClick, danger }: {
  icon: typeof ArrowLeft
  label: string
  trailing?: string
  onClick?: () => void
  danger?: boolean
}) {
  return (
    <button
      onClick={onClick}
      className={clsx(
        'w-full flex items-center gap-3 px-5 py-3.5 transition-colors',
        onClick && 'hover:bg-surface-hi active:bg-surface-hi',
      )}
    >
      <div className={clsx('w-8 h-8 rounded-lg flex items-center justify-center', danger ? 'bg-danger/10' : 'bg-accent/10')}>
        <Icon size={16} className={danger ? 'text-danger' : 'text-accent-sub'} />
      </div>
      <span className="flex-1 text-sm text-on-surface text-left">{label}</span>
      {trailing && <span className="text-xs text-on-surface-faint">{trailing}</span>}
      {onClick && <span className="text-on-surface-faint">›</span>}
    </button>
  )
}

export function SettingsScreen() {
  const navigate = useNavigate()
  const { signOut } = useAuthStore()
  const [theme] = useState<'system' | 'light' | 'dark'>('dark')
  const [autoSave, setAutoSave] = useState(true)

  const themes: Array<{ key: typeof theme; label: string }> = [
    { key: 'system', label: 'Sistema' },
    { key: 'light', label: 'Claro' },
    { key: 'dark', label: 'Oscuro' },
  ]

  const handleSignOut = async () => {
    await signOut()
    navigate('/login')
  }

  return (
    <div className="max-w-lg mx-auto pt-14 pb-4">
      {/* Header */}
      <div className="flex items-center gap-4 px-5 mb-6">
        <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-accent-sub/20 to-accent/10 border border-accent/20 flex items-center justify-center">
          <span className="text-2xl">⚙️</span>
        </div>
        <div>
          <h1 className="text-xl font-bold text-on-surface">Ajustes</h1>
          <p className="text-xs text-on-surface-muted flex items-center gap-1.5">
            <div className="w-4 h-0.5 bg-accent rounded-full" />
            Personaliza tu experiencia
          </p>
        </div>
      </div>

      {/* General */}
      <SettingsSection title="General" />
      <div className="mx-5 rounded-2xl bg-surface border border-outline-faint overflow-hidden">
        <SettingsRow icon={Palette} label="Tema" trailing={themes.find(t => t.key === theme)?.label} />
        <div className="border-t border-outline-faint" />
        <div className="flex items-center gap-3 px-5 py-3.5">
          <div className="w-8 h-8 rounded-lg bg-accent/10 flex items-center justify-center">
            <Database size={16} className="text-accent-sub" />
          </div>
          <span className="flex-1 text-sm text-on-surface">Auto-guardar</span>
          <button
            onClick={() => setAutoSave(!autoSave)}
            className={clsx(
              'w-11 h-6 rounded-full transition-colors relative',
              autoSave ? 'bg-accent' : 'bg-surface-hi',
            )}
          >
            <div className={clsx(
              'absolute top-0.5 w-5 h-5 rounded-full bg-white shadow transition-transform',
              autoSave ? 'translate-x-5.5' : 'translate-x-0.5',
            )} />
          </button>
        </div>
      </div>

      {/* Datos */}
      <SettingsSection title="Datos" />
      <div className="mx-5 rounded-2xl bg-surface border border-outline-faint overflow-hidden">
        <SettingsRow icon={Database} label="Hacer copia de seguridad" />
        <div className="border-t border-outline-faint" />
        <SettingsRow icon={Database} label="Restaurar desde archivo" />
      </div>

      {/* Avanzado */}
      <SettingsSection title="Avanzado" />
      <div className="mx-5 rounded-2xl bg-surface border border-outline-faint overflow-hidden">
        <SettingsRow icon={Trash2} label="Eliminadas recientemente" danger />
      </div>

      {/* PDF */}
      <SettingsSection title="Exportación a PDF" />
      <div className="mx-5 rounded-2xl bg-surface border border-outline-faint overflow-hidden">
        <SettingsRow icon={FileText} label="Densidad de texto" trailing="Normal" />
        <div className="border-t border-outline-faint" />
        <SettingsRow icon={FileText} label="Márgenes" trailing="Normales" />
        <div className="border-t border-outline-faint" />
        <SettingsRow icon={FileText} label="Incluir portada" trailing="Sí" />
        <div className="border-t border-outline-faint" />
        <SettingsRow icon={FileText} label="Marca al pie" trailing="Sí" />
      </div>

      {/* IA */}
      <SettingsSection title="Inteligencia Artificial" />
      <div className="mx-5 rounded-2xl bg-surface border border-outline-faint overflow-hidden">
        <SettingsRow icon={Sparkles} label="Habilitar IA" trailing="No" />
        <div className="border-t border-outline-faint" />
        <SettingsRow icon={ExternalLink} label="Cómo obtener una clave gratuita" />
      </div>

      {/* Acerca de */}
      <SettingsSection title="Acerca de" />
      <div className="mx-5 rounded-2xl bg-surface border border-outline-faint p-4 flex items-center gap-4">
        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-accent-sub/20 to-accent/10 border border-accent/20 flex items-center justify-center">
          <span className="text-xl">🌿</span>
        </div>
        <div>
          <p className="text-sm font-semibold text-on-surface">OreoExperience · Notas</p>
          <p className="text-[10px] text-on-surface-faint">v1.0.0-web · Creado por Elihu Rueda</p>
        </div>
      </div>

      {/* Sign out */}
      <div className="mx-5 mt-4">
        <button
          onClick={handleSignOut}
          className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-surface border border-outline-faint text-danger text-sm font-medium hover:bg-surface-hi transition-colors"
        >
          <LogOut size={16} />
          Cerrar sesión
        </button>
      </div>
    </div>
  )
}
