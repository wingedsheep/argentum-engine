/**
 * Tells the server which top-level page this client is on, so the admin Live overview can say a
 * player is in the deckbuilder or reading the help rather than just "online". Mounted beside the
 * router like {@link MatchmakingLayer}: the socket outlives client-side navigation, and so must this.
 *
 * Only the route's first segment is sent — never ids, deck names, or anything typed. Games, lobbies,
 * drafts, and queues the server already knows about on its own.
 */
import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore.ts'
import { getWebSocket } from '@/store/slices/shared.ts'
import { createReportActivityMessage } from '@/types/messages.ts'
import { pageForPath } from './activityPage'

export default function ActivityReporter() {
  const { pathname } = useLocation()
  // Re-sent when the server (re)assigns an identity, so a fresh session learns the page too.
  const playerId = useGameStore((s) => s.playerId)
  const page = pageForPath(pathname)

  useEffect(() => {
    if (playerId == null) return
    getWebSocket()?.send(createReportActivityMessage(page))
  }, [page, playerId])

  return null
}
