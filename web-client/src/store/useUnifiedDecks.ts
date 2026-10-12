/**
 * One deck library that merges the user's account (cloud) decks with their browser-only (localStorage)
 * decks, tagging each with where it lives. This is what lets the deck browser / picker show a single
 * list with an "Online" badge on the decks that are backed up, instead of two disconnected lists.
 *
 * When signed in, cloud decks are fetched in full (one `?full` request) so they carry their card
 * lists and can render the same rich metadata as local decks. A local deck whose name matches a cloud
 * deck is treated as the same deck (the cloud copy wins) so it isn't shown twice. Mutations route to
 * the right backing store automatically.
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  type DeckDetail,
  deleteDeck as apiDeleteDeck,
  listDeckDetails,
  updateDeck as apiUpdateDeck,
} from '@/api/account'
import {
  type DeckDetailsPatch,
  type SavedDeck,
  type SavedDeckEntry,
  applyDeckDetails,
  useDeckLibrary,
} from '@/store/deckLibrary'
import { useAuthStore } from '@/store/authStore'
import { savedDeckToShared } from '@/store/useSaveDeck'

export interface UnifiedDeck extends SavedDeck {
  /** True = backed up in the account (cloud); false = browser-only (localStorage). */
  online: boolean
  /** Server id, present only for cloud decks (used to route updates/deletes). */
  cloudId?: number
}

function detailToUnified(detail: DeckDetail): UnifiedDeck {
  const d = detail.deck
  // Rebuild per-card entries from the sparse printings map so pinned-printing art still renders.
  const entries: SavedDeckEntry[] | undefined = d.printings
    ? Object.entries(d.cards).map(([name, count]) => {
        const printing = d.printings?.[name]
        return printing ? { name, count, printing } : { name, count }
      })
    : undefined
  return {
    id: `cloud:${detail.id}`,
    cloudId: detail.id,
    online: true,
    name: d.name,
    cards: d.cards,
    ...(d.format ? { format: d.format } : {}),
    ...(d.commander ? { commander: d.commander } : {}),
    ...(d.commanderPrinting ? { commanderPrinting: d.commanderPrinting } : {}),
    ...(d.commander && d.secondCommander ? { secondCommander: d.secondCommander } : {}),
    ...(d.commander && d.secondCommander && d.secondCommanderPrinting
      ? { secondCommanderPrinting: d.secondCommanderPrinting }
      : {}),
    ...(entries ? { entries } : {}),
    ...(d.sideboard && Object.keys(d.sideboard).length > 0 ? { sideboard: d.sideboard } : {}),
    ...(d.companion ? { companion: d.companion } : {}),
    ...(d.note ? { note: d.note } : {}),
    ...(d.coverCard ? { coverCard: d.coverCard } : {}),
    ...(d.coverCard && d.coverImageUri ? { coverImageUri: d.coverImageUri } : {}),
    updatedAt: Date.parse(detail.updatedAt) || 0,
  }
}

export function useUnifiedDecks() {
  const isLoggedIn = useAuthStore((s) => s.status === 'authenticated')
  const localDecks = useDeckLibrary((s) => s.decks)
  const hydrate = useDeckLibrary((s) => s.hydrate)
  const hydrated = useDeckLibrary((s) => s.hydrated)
  const deleteLocal = useDeckLibrary((s) => s.deleteDeck)
  const updateLocal = useDeckLibrary((s) => s.updateDetails)

  const [cloud, setCloud] = useState<DeckDetail[]>([])
  const [loading, setLoading] = useState(false)
  // Whether the cloud half has answered at least once — so "no decks" can be told apart from
  // "not loaded yet" by callers that act on an empty library (the deck picker leaves My Decks).
  const [cloudSettled, setCloudSettled] = useState(false)

  useEffect(() => {
    if (!hydrated) hydrate()
  }, [hydrated, hydrate])

  const reload = useCallback(() => {
    if (!isLoggedIn) {
      setCloud([])
      setCloudSettled(true)
      return
    }
    setLoading(true)
    void listDeckDetails()
      .then(setCloud)
      .catch(() => setCloud([]))
      .finally(() => {
        setLoading(false)
        setCloudSettled(true)
      })
  }, [isLoggedIn])

  useEffect(() => {
    reload()
  }, [reload])

  const decks: UnifiedDeck[] = useMemo(() => {
    const cloudDecks = cloud.map(detailToUnified)
    const cloudNames = new Set(cloudDecks.map((d) => d.name.toLowerCase()))
    const locals: UnifiedDeck[] = localDecks
      .filter((d) => !cloudNames.has(d.name.toLowerCase()))
      .map((d) => ({ ...d, online: false }))
    return [...cloudDecks, ...locals]
  }, [cloud, localDecks])

  const removeDeck = useCallback(
    async (deck: UnifiedDeck) => {
      if (deck.cloudId != null) {
        await apiDeleteDeck(deck.cloudId)
        setCloud((prev) => prev.filter((d) => d.id !== deck.cloudId))
      } else {
        deleteLocal(deck.id)
      }
    },
    [deleteLocal],
  )

  /**
   * Rename the deck, or change its note or cover — the fields that don't alter what gets played.
   * A cloud deck is updated in place (and patched locally first, so the gallery doesn't flicker
   * back to the old value while the request is in flight).
   */
  const updateDetails = useCallback(
    async (deck: UnifiedDeck, patch: DeckDetailsPatch) => {
      if (deck.cloudId != null) {
        const next = applyDeckDetails(deck, patch)
        setCloud((prev) =>
          prev.map((d) => (d.id === deck.cloudId ? { ...d, name: next.name, deck: savedDeckToShared(next) } : d)),
        )
        await apiUpdateDeck(deck.cloudId, savedDeckToShared(next))
        reload()
      } else {
        updateLocal(deck.id, patch)
      }
    },
    [updateLocal, reload],
  )

  /** True once both the browser library and (when signed in) the account's decks have loaded. */
  const settled = hydrated && cloudSettled && !loading

  return { decks, loading, settled, reload, removeDeck, updateDetails, isLoggedIn }
}
