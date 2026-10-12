/**
 * Auth-aware deck saving — the single entry point every "save a deck" button should use.
 *
 * When the user is signed in, the deck goes to their account (cloud), overwriting any same-named
 * deck rather than duplicating it; when anonymous, it goes to the browser library (localStorage).
 * This is what makes "save when logged in saves online" true everywhere: the deckbuilder, the
 * tournament/draft save, and the lobby deck picker all route through here instead of each deciding
 * for themselves (the old tournament path always wrote to localStorage, even when signed in).
 */
import { useCallback } from 'react'
import type { PrintingRef } from '@/types'
import { upsertDeckByName } from '@/api/account'
import type { SharedDeck } from '@/components/deckbuilder/shareDeck'
import { type SavedDeck, useDeckLibrary } from '@/store/deckLibrary'
import { useAuthStore } from '@/store/authStore'

/** What a save needs: everything in a SavedDeck except the storage-assigned id/timestamp. */
export type SaveDeckInput = Omit<SavedDeck, 'id' | 'updatedAt'>

/** Convert a library/draft deck into the cloud wire shape (SharedDeck). */
export function savedDeckToShared(input: SaveDeckInput): SharedDeck {
  const printings: Record<string, PrintingRef> = {}
  for (const entry of input.entries ?? []) {
    if (entry.printing) printings[entry.name] = entry.printing
  }
  return {
    name: input.name,
    cards: input.cards,
    ...(Object.keys(printings).length > 0 ? { printings } : {}),
    ...(input.format ? { format: input.format } : {}),
    ...(input.commander ? { commander: input.commander } : {}),
    ...(input.commanderPrinting ? { commanderPrinting: input.commanderPrinting } : {}),
    ...(input.commander && input.secondCommander ? { secondCommander: input.secondCommander } : {}),
    ...(input.commander && input.secondCommander && input.secondCommanderPrinting
      ? { secondCommanderPrinting: input.secondCommanderPrinting }
      : {}),
    ...(input.sideboard && Object.keys(input.sideboard).length > 0
      ? { sideboard: input.sideboard }
      : {}),
    ...(input.companion ? { companion: input.companion } : {}),
    ...(input.note ? { note: input.note } : {}),
    ...(input.coverCard ? { coverCard: input.coverCard } : {}),
    ...(input.coverCard && input.coverImageUri ? { coverImageUri: input.coverImageUri } : {}),
  }
}

export interface SaveDeckResult {
  /** True when the deck was saved to the account (cloud); false when saved to localStorage. */
  online: boolean
  /** Unified deck id of the saved deck (`cloud:<n>` for account decks, the local id otherwise). */
  id: string
}

/**
 * Returns a `save(input)` that persists to the account when signed in, or localStorage otherwise,
 * plus the current `isLoggedIn` flag so callers can label their button ("Save online" vs "Save").
 */
export function useSaveDeck() {
  const isLoggedIn = useAuthStore((s) => s.status === 'authenticated')
  const saveLocal = useDeckLibrary((s) => s.saveDeck)

  const save = useCallback(
    async (input: SaveDeckInput): Promise<SaveDeckResult> => {
      if (isLoggedIn) {
        const detail = await upsertDeckByName(savedDeckToShared(input))
        return { online: true, id: `cloud:${detail.id}` }
      }
      const saved = saveLocal(input)
      return { online: false, id: saved.id }
    },
    [isLoggedIn, saveLocal],
  )

  return { save, isLoggedIn }
}
