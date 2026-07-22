import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Search, Pin, Trash2, MoreVertical } from 'lucide-react'
import { useAuthStore } from '@/stores/authStore'
import { useNotesStore, createNote, softDeleteNote, togglePin } from '@/stores/notesStore'
import { CATEGORIES, type NoteCategory } from '@/types'
import clsx from 'clsx'

const categoryFilters: Array<{ key: NoteCategory | 'all'; label: string }> = [
  { key: 'all', label: 'Todos' },
  { key: 'discurso', label: 'Discursos' },
  { key: 'consideracion', label: 'Consideraciones' },
  { key: 'servicio_campo', label: 'Servicio' },
  { key: 'general', label: 'General' },
]

function formatDate(ms: number): string {
  const d = new Date(ms)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return 'Ahora'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}d`
  return d.toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })
}

function groupByMonth(notes: Array<{ updatedAt: number }>) {
  const groups = new Map<string, number[]>()
  const now = new Date()
  notes.forEach((n, i) => {
    const d = new Date(n.updatedAt)
    const key = d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear()
      ? 'Este mes'
      : d.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(i)
  })
  return groups
}

export function HomeScreen() {
  const user = useAuthStore(s => s.user)
  const searchQuery = useNotesStore(s => s.searchQuery)
  const setSearchQuery = useNotesStore(s => s.setSearchQuery)
  const categoryFilter = useNotesStore(s => s.categoryFilter)
  const setCategoryFilter = useNotesStore(s => s.setCategoryFilter)
  const sortBy = useNotesStore(s => s.sortBy)
  const allNotes = useNotesStore(s => s.notes)
  const [menuOpen, setMenuOpen] = useState<string | null>(null)
  const navigate = useNavigate()

  const notes = useMemo(() => {
    let result = [...allNotes]
    if (categoryFilter !== 'all') {
      result = result.filter(n => n.category === categoryFilter)
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase()
      result = result.filter(n =>
        n.title.toLowerCase().includes(q) ||
        n.notes.toLowerCase().includes(q) ||
        n.tags.toLowerCase().includes(q)
      )
    }
    result.sort((a, b) => {
      if (a.pinned !== b.pinned) return a.pinned ? -1 : 1
      switch (sortBy) {
        case 'updated': return b.updatedAt - a.updatedAt
        case 'created': return b.createdAt - a.createdAt
        case 'title': return a.title.localeCompare(b.title)
      }
    })
    return result
  }, [allNotes, searchQuery, categoryFilter, sortBy])
  const grouped = groupByMonth(notes)

  const handleNew = async (cat: NoteCategory) => {
    if (!user) return
    const id = await createNote(user.uid, cat)
    navigate(`/editor/${id}`)
  }

  const handleTogglePin = async (id: string, pinned: boolean) => {
    if (!user) return
    await togglePin(user.uid, id, pinned)
    setMenuOpen(null)
  }

  const handleDelete = async (id: string) => {
    if (!user) return
    await softDeleteNote(user.uid, id)
    setMenuOpen(null)
  }

  return (
    <div className="max-w-lg mx-auto px-5 pt-14 pb-4">
      <h1 className="text-3xl font-bold text-on-surface tracking-tight mb-1">
        OreoExperience
      </h1>
      <p className="text-sm text-on-surface-muted mb-6 flex items-center gap-2">
        <span className="w-6 h-0.5 bg-accent rounded-full inline-block" />
        Notas
      </p>

      {/* Search */}
      <div className="relative mb-5">
        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-faint" />
        <input
          type="text"
          placeholder="Buscar..."
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-surface border border-outline-faint text-sm text-on-surface placeholder:text-on-surface-faint focus:outline-none focus:border-accent/40 transition-colors"
        />
      </div>

      {/* Category chips */}
      <div className="flex gap-2 overflow-x-auto pb-3 mb-4 no-scrollbar">
        {categoryFilters.map(f => (
          <button
            key={f.key}
            onClick={() => setCategoryFilter(f.key)}
            className={clsx(
              'px-3.5 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all',
              categoryFilter === f.key
                ? 'bg-accent text-white'
                : 'bg-surface text-on-surface-muted border border-outline-faint hover:border-accent/30',
            )}
          >
            {f.label}
          </button>
        ))}
      </div>

      {/* Notes list */}
      {notes.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <div className="w-16 h-16 rounded-full bg-surface flex items-center justify-center mb-4">
            <span className="text-2xl">📝</span>
          </div>
          <p className="text-on-surface-muted text-sm">
            {searchQuery ? 'Sin resultados' : 'No hay notas todavía'}
          </p>
          <p className="text-on-surface-faint text-xs mt-1">
            Toca + para crear la primera
          </p>
        </div>
      ) : (
        Array.from(grouped.entries()).map(([month, indices]) => (
          <div key={month} className="mb-6">
            <h2 className="text-xs font-semibold text-on-surface-faint uppercase tracking-wider mb-3">
              {month}
            </h2>
            <div className="space-y-2">
              {indices.map(idx => {
                const note = notes[idx]
                return (
                  <div
                    key={note.id}
                    className="relative flex items-start gap-3 p-3.5 rounded-2xl bg-surface border border-outline-faint hover:border-accent/20 transition-all cursor-pointer group"
                    onClick={() => navigate(`/editor/${note.id}`)}
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        {note.pinned && <Pin size={12} className="text-accent shrink-0" />}
                        <h3 className="text-sm font-semibold text-on-surface truncate">
                          {note.title || 'Nota nueva'}
                        </h3>
                      </div>
                      <p className="text-xs text-on-surface-muted truncate">
                        {note.notes.replace(/<[^>]*>/g, '').slice(0, 80) || CATEGORIES[note.category as NoteCategory]?.short}
                      </p>
                    </div>
                    <span className="text-[10px] text-on-surface-faint shrink-0 mt-0.5">
                      {formatDate(note.updatedAt)}
                    </span>

                    {/* Context menu trigger */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        setMenuOpen(menuOpen === note.id ? null : note.id)
                      }}
                      className="absolute top-2 right-2 p-1 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-surface-hi transition-all"
                    >
                      <MoreVertical size={14} className="text-on-surface-faint" />
                    </button>

                    {menuOpen === note.id && (
                      <div
                        className="absolute top-8 right-2 z-10 w-40 bg-bg-card border border-outline-faint rounded-xl shadow-xl shadow-black/30 overflow-hidden"
                        onClick={e => e.stopPropagation()}
                      >
                        <button
                          onClick={() => handleTogglePin(note.id, note.pinned)}
                          className="w-full flex items-center gap-2 px-3 py-2.5 text-xs text-on-surface hover:bg-surface-hi transition-colors"
                        >
                          <Pin size={14} />
                          {note.pinned ? 'Desfijar' : 'Fijar'}
                        </button>
                        <button
                          onClick={() => handleDelete(note.id)}
                          className="w-full flex items-center gap-2 px-3 py-2.5 text-xs text-danger hover:bg-surface-hi transition-colors"
                        >
                          <Trash2 size={14} />
                          Eliminar
                        </button>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        ))
      )}

      {/* FAB */}
      <button
        onClick={() => handleNew('general')}
        className="fixed bottom-24 right-6 w-14 h-14 rounded-full bg-accent flex items-center justify-center shadow-lg shadow-accent/30 hover:scale-105 active:scale-95 transition-all z-40"
      >
        <Plus className="w-7 h-7 text-white" />
      </button>
    </div>
  )
}
