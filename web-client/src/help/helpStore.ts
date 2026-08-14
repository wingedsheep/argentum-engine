/**
 * UI state for the in-game help drawer.
 *
 * The `/help` page and the drawer render the same topics from `topics.ts`; the drawer exists only
 * because navigating away from a game would drop the WebSocket. `mounted` lets {@link HelpTip}'s
 * "Read more" pick the right destination without every call site having to know where it is.
 */
import { create } from 'zustand'

interface HelpUiState {
  /** True while a HelpDrawer is on screen somewhere (i.e. we are in a game). */
  mounted: boolean
  /** Topic to scroll to when the drawer opens; null when the drawer is closed. */
  openTopicId: string | null
  isOpen: boolean
  setMounted: (mounted: boolean) => void
  openDrawer: (topicId?: string) => void
  closeDrawer: () => void
}

export const useHelpUi = create<HelpUiState>((set) => ({
  mounted: false,
  openTopicId: null,
  isOpen: false,
  setMounted: (mounted) => set(mounted ? { mounted } : { mounted, isOpen: false }),
  openDrawer: (topicId) => set({ isOpen: true, openTopicId: topicId ?? null }),
  closeDrawer: () => set({ isOpen: false, openTopicId: null }),
}))
