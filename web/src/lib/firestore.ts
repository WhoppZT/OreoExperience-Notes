import {
  collection, addDoc, updateDoc, deleteDoc, doc,
  query, where, onSnapshot,
  getDocs, writeBatch,
} from 'firebase/firestore'
import { db } from './firebase'
import type { Discurso, RegistroCampo } from '@/types'

function userCollection(uid: string, col: string) {
  return collection(db, 'users', uid, col)
}

// ── Notes ──────────────────────────────────────────────────────

export function observeNotes(uid: string, cb: (notes: Discurso[]) => void) {
  const q = query(userCollection(uid, 'discursos'))
  return onSnapshot(q, snap => {
    const notes = snap.docs
      .map(d => ({ id: d.id, ...d.data() } as Discurso))
      .filter(n => n.deletedAt == null)
    cb(notes)
  })
}

export function observeTrashedNotes(uid: string, cb: (notes: Discurso[]) => void) {
  const q = query(userCollection(uid, 'discursos'))
  return onSnapshot(q, snap => {
    const notes = snap.docs
      .map(d => ({ id: d.id, ...d.data() } as Discurso))
      .filter(n => n.deletedAt != null)
    cb(notes)
  })
}

export async function createNote(uid: string, data: Omit<Discurso, 'id'>) {
  const ref = await addDoc(userCollection(uid, 'discursos'), {
    ...data,
    createdAt: Date.now(),
    updatedAt: Date.now(),
  })
  return ref.id
}

export async function updateNote(uid: string, id: string, data: Partial<Discurso>) {
  await updateDoc(doc(db, 'users', uid, 'discursos', id), {
    ...data,
    updatedAt: Date.now(),
  })
}

export async function softDeleteNote(uid: string, id: string) {
  await updateDoc(doc(db, 'users', uid, 'discursos', id), {
    deletedAt: Date.now(),
    updatedAt: Date.now(),
  })
}

export async function restoreNote(uid: string, id: string) {
  await updateDoc(doc(db, 'users', uid, 'discursos', id), {
    deletedAt: null,
    updatedAt: Date.now(),
  })
}

export async function permanentDeleteNote(uid: string, id: string) {
  await deleteDoc(doc(db, 'users', uid, 'discursos', id))
}

export async function purgeOldTrashed(uid: string, olderThan: number) {
  const snap = await getDocs(userCollection(uid, 'discursos'))
  const batch = writeBatch(db)
  let count = 0
  snap.forEach(d => {
    const data = d.data()
    if (data.deletedAt && data.deletedAt < olderThan) {
      batch.delete(d.ref)
      count++
    }
  })
  if (count > 0) await batch.commit()
  return count
}

// ── Field Service Records ──────────────────────────────────────

export function observeRecords(uid: string, cb: (records: RegistroCampo[]) => void) {
  const q = query(userCollection(uid, 'registros_campo'))
  return onSnapshot(q, snap => {
    cb(snap.docs.map(d => ({ id: d.id, ...d.data() } as RegistroCampo)))
  })
}

export async function upsertRecord(uid: string, data: Omit<RegistroCampo, 'id'>) {
  const snap = await getDocs(query(
    userCollection(uid, 'registros_campo'),
    where('dateMillis', '==', data.dateMillis),
  ))
  if (!snap.empty) {
    await updateDoc(snap.docs[0].ref, { ...data, updatedAt: Date.now() })
    return snap.docs[0].id
  } else {
    const ref = await addDoc(userCollection(uid, 'registros_campo'), {
      ...data,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    })
    return ref.id
  }
}

export async function deleteRecord(uid: string, id: string) {
  await deleteDoc(doc(db, 'users', uid, 'registros_campo', id))
}
