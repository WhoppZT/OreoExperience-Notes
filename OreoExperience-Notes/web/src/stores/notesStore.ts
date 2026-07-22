import { create } from 'zustand'
import type { Discurso, NoteCategory, SortBy } from '@/types'
import * as fs from '@/lib/firestore'

interface NotesState {
  notes: Discurso[]
  trashed: Discurso[]
  sortBy: SortBy
  searchQuery: string
  categoryFilter: NoteCategory | 'all'
  setNotes: (n: Discurso[]) => void
  setTrashed: (n: Discurso[]) => void
  setSortBy: (s: SortBy) => void
  setSearchQuery: (q: string) => void
  setCategoryFilter: (c: NoteCategory | 'all') => void
  filteredNotes: () => Discurso[]
}

export const useNotesStore = create<NotesState>((set, get) => ({
  notes: [],
  trashed: [],
  sortBy: 'updated',
  searchQuery: '',
  categoryFilter: 'all',
  setNotes: (n: Discurso[]) => set({ notes: n }),
  setTrashed: (n: Discurso[]) => set({ trashed: n }),
  setSortBy: (s: SortBy) => set({ sortBy: s }),
  setSearchQuery: (q: string) => set({ searchQuery: q }),
  setCategoryFilter: (c: NoteCategory | 'all') => set({ categoryFilter: c }),
  filteredNotes: () => {
    const { notes, sortBy, searchQuery, categoryFilter } = get()
    let result = [...notes]
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
  },
}))

export async function createNote(uid: string, category: NoteCategory): Promise<string> {
  return fs.createNote(uid, {
    title: '',
    scriptures: '',
    tags: '',
    notes: '',
    createdAt: Date.now(),
    updatedAt: Date.now(),
    targetDurationSec: 0,
    pinned: false,
    deletedAt: null,
    category,
    userId: uid,
  })
}

export async function updateNote(uid: string, id: string, data: Partial<Discurso>) {
  return fs.updateNote(uid, id, data)
}

export async function softDeleteNote(uid: string, id: string) {
  return fs.softDeleteNote(uid, id)
}

export async function restoreNote(uid: string, id: string) {
  return fs.restoreNote(uid, id)
}

export async function permanentDeleteNote(uid: string, id: string) {
  return fs.permanentDeleteNote(uid, id)
}

export async function togglePin(uid: string, id: string, pinned: boolean) {
  return fs.updateNote(uid, id, { pinned: !pinned })
}
