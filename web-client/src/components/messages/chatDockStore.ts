/**
 * Shared state between the chat button (in each screen's top-bar chrome, beside fullscreen) and the
 * chat dock that renders the panel. The button that's on screen registers itself as the anchor the
 * panel and the new-message preview drop down from; the dock owns everything else.
 */
import { create } from 'zustand'
import { useHomeVisible } from '@/hooks/useHomeVisible'
import { usePreferences } from '@/store/preferencesStore'

export type ChatView =
  | { readonly kind: 'list' }
  | { readonly kind: 'new' }
  | { readonly kind: 'chat'; readonly accountId: string }

interface ChatDockState {
  open: boolean
  view: ChatView
  /** Chat buttons on screen, newest last — the last one anchors the panel. */
  anchors: HTMLElement[]

  /** Open on [view], or where the panel was left. */
  show: (view?: ChatView) => void
  toggle: () => void
  close: () => void
  setView: (view: ChatView) => void
  addAnchor: (el: HTMLElement) => void
  removeAnchor: (el: HTMLElement) => void
}

export const useChatDock = create<ChatDockState>((set, get) => ({
  open: false,
  view: { kind: 'list' },
  anchors: [],

  show: (view) => set({ open: true, ...(view ? { view } : {}) }),
  toggle: () => set({ open: !get().open }),
  close: () => set({ open: false }),
  setView: (view) => set({ view }),
  addAnchor: (el) => set((s) => ({ anchors: [...s.anchors.filter((a) => a !== el), el] })),
  removeAnchor: (el) => set((s) => ({ anchors: s.anchors.filter((a) => a !== el) })),
}))

/**
 * Whether new messages should announce themselves here: always on the home screen, elsewhere only
 * with "Message alerts away from home" on.
 */
export function useMessageAlertsOn(): boolean {
  const homeVisible = useHomeVisible()
  const alertsAway = usePreferences((s) => s.prefs.messages.alertsAway)
  return homeVisible || alertsAway
}
