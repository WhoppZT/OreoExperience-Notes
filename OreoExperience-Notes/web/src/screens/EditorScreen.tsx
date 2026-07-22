import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft, Plus, Trash2, GripVertical, Check, Square,
  Bold, Italic, List, ListOrdered, Image, Type, CheckSquare,
  ChevronDown,
} from 'lucide-react'
import { useAuthStore } from '@/stores/authStore'
import { useNotesStore, updateNote } from '@/stores/notesStore'
import {
  type NoteBlock, type ChecklistItem,
  decodeBlocks, encodeBlocks,
  newTextBlock, newChecklistBlock, newImageBlock, newId,
} from '@/types/blocks'
import { CATEGORIES, type NoteCategory } from '@/types'
import clsx from 'clsx'

export function EditorScreen() {
  const { id } = useParams<{ id: string }>()
  const user = useAuthStore(s => s.user)
  const notes = useNotesStore(s => s.notes)
  const navigate = useNavigate()

  const note = notes.find(n => n.id === id)
  const [title, setTitle] = useState('')
  const [blocks, setBlocks] = useState<NoteBlock[]>([newTextBlock()])
  const [category, setCategory] = useState<NoteCategory>('general')
  const [showDelete, setShowDelete] = useState(false)
  const [showAddMenu, setShowAddMenu] = useState(false)
  const addMenuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (note) {
      setTitle(note.title)
      setBlocks(decodeBlocks(note.notes))
      setCategory(note.category)
    }
  }, [note?.id])

  const save = useCallback(async () => {
    if (!user || !id) return
    await updateNote(user.uid, id, {
      title,
      notes: encodeBlocks(blocks),
      category,
    })
  }, [user, id, title, blocks, category])

  const handleBack = async () => {
    await save()
    navigate(-1)
  }

  const handleDelete = async () => {
    if (!user || !id) return
    const { softDeleteNote } = await import('@/stores/notesStore')
    await softDeleteNote(user.uid, id)
    navigate('/')
  }

  // ── Block operations ──

  const updateBlock = (idx: number, block: NoteBlock) => {
    setBlocks(prev => prev.map((b, i) => i === idx ? block : b))
  }

  const removeBlock = (idx: number) => {
    setBlocks(prev => {
      const next = prev.filter((_, i) => i !== idx)
      return next.length === 0 ? [newTextBlock()] : next
    })
  }

  const addBlockAfter = (idx: number, block: NoteBlock) => {
    setBlocks(prev => {
      const next = [...prev]
      next.splice(idx + 1, 0, block)
      return next
    })
    setShowAddMenu(false)
  }

  const moveBlock = (from: number, to: number) => {
    if (to < 0 || to >= blocks.length) return
    setBlocks(prev => {
      const next = [...prev]
      const [moved] = next.splice(from, 1)
      next.splice(to, 0, moved)
      return next
    })
  }

  // ── Image handling ──

  const handleImageUpload = (idx: number, file: File) => {
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = reader.result as string
      updateBlock(idx, newImageBlock(dataUrl, file.name))
    }
    reader.readAsDataURL(file)
  }

  // ── Click outside add menu ──
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (addMenuRef.current && !addMenuRef.current.contains(e.target as Node)) {
        setShowAddMenu(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  if (!note) {
    return (
      <div className="min-h-dvh flex items-center justify-center bg-bg">
        <div className="w-8 h-8 border-2 border-accent border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="min-h-dvh flex flex-col bg-bg">
      {/* Top bar */}
      <div className="flex items-center justify-between px-4 pt-12 pb-3 border-b border-outline-faint">
        <button onClick={handleBack} className="flex items-center gap-1 text-accent-sub hover:text-accent text-sm font-medium transition-colors">
          <ArrowLeft size={18} />
          Volver
        </button>
        <div className="flex items-center gap-2">
          <button onClick={() => setShowDelete(true)} className="p-2 rounded-lg hover:bg-surface-hi transition-colors">
            <Trash2 size={18} className="text-danger" />
          </button>
        </div>
      </div>

      {/* Category selector */}
      <div className="flex gap-2 px-5 py-3 overflow-x-auto">
        {(Object.entries(CATEGORIES) as [NoteCategory, { label: string }][]).map(([key, { label }]) => (
          <button
            key={key}
            onClick={() => setCategory(key)}
            className={clsx(
              'px-3 py-1 rounded-full text-xs font-medium whitespace-nowrap transition-all',
              category === key
                ? 'bg-accent text-white'
                : 'bg-surface text-on-surface-muted border border-outline-faint',
            )}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Title */}
      <div className="px-5 pt-2">
        <input
          type="text"
          placeholder="Título"
          value={title}
          onChange={e => setTitle(e.target.value)}
          className="w-full text-2xl font-bold text-on-surface bg-transparent placeholder:text-on-surface-faint focus:outline-none"
        />
      </div>

      {/* Blocks */}
      <div className="flex-1 px-5 py-4 space-y-2">
        {blocks.map((block, idx) => (
          <div key={block.id} className="group relative">
            {/* Block controls */}
            <div className="absolute -left-10 top-1 flex flex-col items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                onClick={() => moveBlock(idx, idx - 1)}
                disabled={idx === 0}
                className="p-0.5 text-on-surface-faint hover:text-on-surface disabled:opacity-20 transition-colors"
              >
                <ChevronDown size={12} className="rotate-180" />
              </button>
              <GripVertical size={14} className="text-on-surface-faint cursor-grab" />
              <button
                onClick={() => moveBlock(idx, idx + 1)}
                disabled={idx === blocks.length - 1}
                className="p-0.5 text-on-surface-faint hover:text-on-surface disabled:opacity-20 transition-colors"
              >
                <ChevronDown size={12} />
              </button>
            </div>

            {block.type === 'text' && (
              <TextBlockEditor
                block={block}
                onChange={(b) => updateBlock(idx, b)}
                onRemove={() => removeBlock(idx)}
                onAddBelow={() => addBlockAfter(idx, newTextBlock())}
              />
            )}

            {block.type === 'checklist' && (
              <ChecklistBlockEditor
                block={block}
                onChange={(b) => updateBlock(idx, b)}
                onRemove={() => removeBlock(idx)}
                onAddBelow={() => addBlockAfter(idx, newTextBlock())}
              />
            )}

            {block.type === 'image' && (
              <ImageBlockEditor
                block={block}
                onRemove={() => removeBlock(idx)}
                onUpload={(file) => handleImageUpload(idx, file)}
              />
            )}
          </div>
        ))}

        {/* Add block button */}
        <div className="relative" ref={addMenuRef}>
          <button
            onClick={() => setShowAddMenu(!showAddMenu)}
            className="flex items-center gap-2 px-3 py-2 rounded-xl text-xs text-on-surface-faint hover:text-on-surface-muted hover:bg-surface-hi transition-all"
          >
            <Plus size={14} />
            Agregar bloque
          </button>

          {showAddMenu && (
            <div className="absolute left-0 top-full mt-1 z-10 w-48 bg-bg-card border border-outline-faint rounded-xl shadow-xl shadow-black/30 overflow-hidden">
              <AddBlockMenuItem
                icon={<Type size={14} />}
                label="Texto"
                onClick={() => addBlockAfter(blocks.length - 1, newTextBlock())}
              />
              <AddBlockMenuItem
                icon={<CheckSquare size={14} />}
                label="Checklist"
                onClick={() => addBlockAfter(blocks.length - 1, newChecklistBlock())}
              />
              <AddBlockMenuItem
                icon={<Image size={14} />}
                label="Imagen"
                onClick={() => {
                  const input = document.createElement('input')
                  input.type = 'file'
                  input.accept = 'image/*'
                  input.onchange = (e) => {
                    const file = (e.target as HTMLInputElement).files?.[0]
                    if (file) {
                      const reader = new FileReader()
                      reader.onload = () => {
                        addBlockAfter(blocks.length - 1, newImageBlock(reader.result as string, file.name))
                      }
                      reader.readAsDataURL(file)
                    }
                  }
                  input.click()
                  setShowAddMenu(false)
                }}
              />
            </div>
          )}
        </div>
      </div>

      {/* Delete confirmation */}
      {showDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={() => setShowDelete(false)}>
          <div className="w-80 bg-bg-card rounded-2xl p-5 space-y-3" onClick={e => e.stopPropagation()}>
            <h3 className="text-base font-bold text-on-surface">Eliminar nota</h3>
            <p className="text-sm text-on-surface-muted">¿Estás seguro? Se moverá a la papelera.</p>
            <div className="flex gap-3 pt-2">
              <button onClick={() => setShowDelete(false)} className="flex-1 py-2.5 rounded-xl bg-surface-hi text-on-surface-muted text-sm font-medium">Cancelar</button>
              <button onClick={handleDelete} className="flex-1 py-2.5 rounded-xl bg-danger text-white text-sm font-medium">Eliminar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Text Block ──

function TextBlockEditor({
  block,
  onChange,
  onRemove,
  onAddBelow,
}: {
  block: import('@/types/blocks').TextBlock
  onChange: (b: NoteBlock) => void
  onRemove: () => void
  onAddBelow: () => void
}) {
  const ref = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (ref.current) {
      ref.current.style.height = 'auto'
      ref.current.style.height = ref.current.scrollHeight + 'px'
    }
  }, [block.markdown])

  const insertMarkdown = (prefix: string, suffix: string) => {
    const el = ref.current
    if (!el) return
    const start = el.selectionStart
    const end = el.selectionEnd
    const selected = block.markdown.slice(start, end)
    const newText = block.markdown.slice(0, start) + prefix + selected + suffix + block.markdown.slice(end)
    onChange({ ...block, markdown: newText })
    setTimeout(() => {
      el.focus()
      el.selectionStart = start + prefix.length
      el.selectionEnd = end + prefix.length
    }, 0)
  }

  return (
    <div className="rounded-xl bg-surface/50 border border-outline-faint focus-within:border-accent/30 transition-colors">
      {/* Mini toolbar */}
      <div className="flex items-center gap-0.5 px-2 py-1 border-b border-outline-faint">
        <ToolbarBtn icon={<Bold size={13} />} onClick={() => insertMarkdown('**', '**')} />
        <ToolbarBtn icon={<Italic size={13} />} onClick={() => insertMarkdown('*', '*')} />
        <div className="w-px h-4 bg-outline-faint mx-1" />
        <ToolbarBtn icon={<List size={13} />} onClick={() => insertMarkdown('- ', '')} />
        <ToolbarBtn icon={<ListOrdered size={13} />} onClick={() => insertMarkdown('1. ', '')} />
        <div className="flex-1" />
        <button onClick={onAddBelow} className="p-1 text-on-surface-faint hover:text-accent-sub transition-colors">
          <Plus size={13} />
        </button>
        <button onClick={onRemove} className="p-1 text-on-surface-faint hover:text-danger transition-colors">
          <Trash2 size={13} />
        </button>
      </div>

      <textarea
        ref={ref}
        value={block.markdown}
        onChange={e => onChange({ ...block, markdown: e.target.value })}
        placeholder="Escribe aquí..."
        rows={1}
        className="w-full px-3 py-2.5 text-sm text-on-surface bg-transparent placeholder:text-on-surface-faint focus:outline-none resize-none leading-relaxed"
      />
    </div>
  )
}

// ── Checklist Block ──

function ChecklistBlockEditor({
  block,
  onChange,
  onRemove,
  onAddBelow,
}: {
  block: import('@/types/blocks').ChecklistBlock
  onChange: (b: NoteBlock) => void
  onRemove: () => void
  onAddBelow: () => void
}) {
  const updateItem = (itemId: string, data: Partial<ChecklistItem>) => {
    onChange({
      ...block,
      items: block.items.map(item => item.id === itemId ? { ...item, ...data } : item),
    })
  }

  const removeItem = (itemId: string) => {
    const next = block.items.filter(item => item.id !== itemId)
    if (next.length === 0) {
      onRemove()
    } else {
      onChange({ ...block, items: next })
    }
  }

  const addItem = (afterId: string) => {
    const idx = block.items.findIndex(i => i.id === afterId)
    const next = [...block.items]
    next.splice(idx + 1, 0, { id: newId(), text: '', checked: false })
    onChange({ ...block, items: next })
    setTimeout(() => {
      const inputs = document.querySelectorAll(`[data-checklist="${block.id}"] input[type="text"]`)
      const newInput = inputs[idx + 1] as HTMLInputElement
      newInput?.focus()
    }, 0)
  }

  return (
    <div className="rounded-xl bg-surface/50 border border-outline-faint p-2">
      <div className="flex items-center justify-between mb-1">
        <span className="text-[10px] text-on-surface-faint font-medium uppercase tracking-wider">Checklist</span>
        <div className="flex gap-1">
          <button onClick={onAddBelow} className="p-1 text-on-surface-faint hover:text-accent-sub transition-colors">
            <Plus size={13} />
          </button>
          <button onClick={onRemove} className="p-1 text-on-surface-faint hover:text-danger transition-colors">
            <Trash2 size={13} />
          </button>
        </div>
      </div>

      <div data-checklist={block.id} className="space-y-1">
        {block.items.map(item => (
          <div key={item.id} className="flex items-center gap-2 group">
            <button
              onClick={() => updateItem(item.id, { checked: !item.checked })}
              className="shrink-0"
            >
              {item.checked
                ? <Check size={18} className="text-accent bg-accent rounded" style={{ padding: 2 }} />
                : <Square size={18} className="text-on-surface-faint" />
              }
            </button>
            <input
              type="text"
              value={item.text}
              onChange={e => updateItem(item.id, { text: e.target.value })}
              onKeyDown={e => {
                if (e.key === 'Enter') {
                  e.preventDefault()
                  addItem(item.id)
                } else if (e.key === 'Backspace' && !item.text && block.items.length > 1) {
                  removeItem(item.id)
                }
              }}
              placeholder="Elemento..."
              className={clsx(
                'flex-1 text-sm bg-transparent focus:outline-none',
                item.checked ? 'text-on-surface-faint line-through' : 'text-on-surface',
              )}
            />
            <button
              onClick={() => removeItem(item.id)}
              className="p-0.5 text-on-surface-faint hover:text-danger opacity-0 group-hover:opacity-100 transition-all"
            >
              <Trash2 size={12} />
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}

// ── Image Block ──

function ImageBlockEditor({
  block,
  onRemove,
  onUpload,
}: {
  block: import('@/types/blocks').ImageBlock
  onRemove: () => void
  onUpload: (file: File) => void
}) {
  return (
    <div className="rounded-xl bg-surface/50 border border-outline-faint overflow-hidden">
      <div className="flex items-center justify-between px-2 py-1 border-b border-outline-faint">
        <span className="text-[10px] text-on-surface-faint font-medium uppercase tracking-wider">Imagen</span>
        <div className="flex gap-1">
          <label className="p-1 text-on-surface-faint hover:text-accent-sub transition-colors cursor-pointer">
            <Image size={13} />
            <input type="file" accept="image/*" className="hidden" onChange={e => {
              const file = e.target.files?.[0]
              if (file) onUpload(file)
            }} />
          </label>
          <button onClick={onRemove} className="p-1 text-on-surface-faint hover:text-danger transition-colors">
            <Trash2 size={13} />
          </button>
        </div>
      </div>

      {block.dataUrl ? (
        <img src={block.dataUrl} alt={block.fileName} className="w-full max-h-80 object-contain" />
      ) : (
        <label className="flex flex-col items-center justify-center py-10 cursor-pointer hover:bg-surface-hi transition-colors">
          <Image size={24} className="text-on-surface-faint mb-2" />
          <span className="text-xs text-on-surface-faint">Subir imagen</span>
          <input type="file" accept="image/*" className="hidden" onChange={e => {
            const file = e.target.files?.[0]
            if (file) onUpload(file)
          }} />
        </label>
      )}
    </div>
  )
}

// ── Helpers ──

function ToolbarBtn({ icon, onClick }: { icon: React.ReactNode; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="p-1.5 rounded-md text-on-surface-faint hover:text-on-surface hover:bg-surface-hi transition-colors"
    >
      {icon}
    </button>
  )
}

function AddBlockMenuItem({ icon, label, onClick }: { icon: React.ReactNode; label: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="w-full flex items-center gap-2.5 px-3 py-2.5 text-xs text-on-surface hover:bg-surface-hi transition-colors"
    >
      <span className="text-accent-sub">{icon}</span>
      {label}
    </button>
  )
}
