import { create, type StateCreator } from 'zustand'
import { onAuthStateChanged, signInWithPopup, signOut as firebaseSignOut, type User } from 'firebase/auth'
import { auth, googleProvider } from '@/lib/firebase'

interface AuthState {
  user: User | null
  loading: boolean
  init: () => () => void
  signInWithGoogle: () => Promise<void>
  signOut: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set: Parameters<StateCreator<AuthState>>[0]) => ({
  user: null,
  loading: true,
  init: () => {
    const unsub = onAuthStateChanged(auth, (user) => {
      set({ user, loading: false })
    })
    return unsub
  },
  signInWithGoogle: async () => {
    await signInWithPopup(auth, googleProvider)
  },
  signOut: async () => {
    await firebaseSignOut(auth)
  },
}))
