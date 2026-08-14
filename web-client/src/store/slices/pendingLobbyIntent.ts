/**
 * What a lobby that does not exist yet is supposed to become.
 *
 * This is `pendingDeckTab.ts` generalised. That module existed because Random pool is not a lobby
 * setting — it is the deck picker's Random tab, whose empty list is the server's own "roll me one"
 * signal — so anything that *promises* a random pool has to move the picker, and everything that
 * promises one creates the lobby from outside it. Applying a saved setup has the same shape and
 * three more things to hand across: which deck to preselect, whether to start once configured, and
 * anything that couldn't be restored.
 *
 * Two separate handoffs live here, because they are consumed by different things at different times:
 *
 * - **{@link PendingLobbyIntent}** — read once by the next `LobbyScreen` to mount.
 * - **{@link PendingLobbyApply}** — flushed by the first server broadcast for the new lobby.
 *
 * ## Why the settings wait for a broadcast instead of being fired straight after the create
 *
 * `useLobbyCommands.recreate` gets away with fire-and-forget because it sends nothing but a create.
 * Here we are sending twenty fields whose loss is silent and immediately visible to the player, and
 * the server keys `updateLobbySettings` on `identity.currentLobbyId` — which is only set once
 * `handleCreate` has run. Waiting for the first `lobbyUpdate`/`quickGameLobbyState` costs one round
 * trip and removes the whole class of race.
 *
 * ## Why one omnibus message rather than a field at a time
 *
 * `LobbyHandler.handleUpdateLobbySettings` is *already* ordered for a whole bag: its own comments
 * read "apply after format change" and "apply after boosterCount", the format branch honours a
 * `setCodes` in the same message, and `rules` lands before the Commander knobs that gate on it. A
 * format change resets `boosterCount`, `picksPerRound`, `chaosBoosters` and recalculates the booster
 * distribution — so **sending these one at a time would be wrong**, not merely slower. The cube is
 * the one exception and goes first, alone: the handler resolves `cubeCards` immediately and
 * `return`s on a card it can't find, which would discard everything else in the bag.
 *
 * Module state rather than store state, for the same reason `pendingDeckTab` was: it is consumed
 * exactly once and nothing re-renders on it. Same shape as `loadLobbyId` in `shared.ts`.
 */
import type { DeckPickerTab } from '@/components/ui/DeckPicker'
import type { AiDeckSpec, UpdateLobbySettingsMessage } from '@/types'

/** Handed to the next `LobbyScreen` to mount. */
export interface PendingLobbyIntent {
  /** Which tab the deck picker opens on. */
  readonly deckTab?: DeckPickerTab
  /** A saved deck to preselect, by name — resolved against the merged deck library. */
  readonly deckName?: string
  /** Ready up / start as soon as the lobby is configured. Only honoured when nobody can join. */
  readonly autoStart?: boolean
  /** Anything a setup couldn't restore, shown once in the lobby rather than swallowed. */
  readonly notes?: readonly string[]
}

/** The cube half of a queued apply — its own message, sent first; see the module comment. */
export type PendingCubeUpdate = Required<
  Pick<UpdateLobbySettingsMessage,
    'cubeCards' | 'cubeName' | 'cubeBasicLandSetCode' | 'packSize' | 'cubePoolPlay'>
>

/** The omnibus bag, minus anything the create message already carried. */
export type PendingSettingsUpdate = Omit<UpdateLobbySettingsMessage, 'type'>

export interface PendingLobbyApply {
  /** Sent before {@link PendingLobbyApply.settings}; see the module comment. */
  readonly cube?: PendingCubeUpdate
  readonly settings?: PendingSettingsUpdate
  /** Quick lobbies take their AI deck as its own message, after the lobby exists. */
  readonly aiDeck?: AiDeckSpec
  /** Added last: switching `gameMode` with AI already seated is rejected outright. */
  readonly aiSeats: number
}

let intent: PendingLobbyIntent | null = null
let apply: PendingLobbyApply | null = null

export function setPendingLobbyIntent(next: PendingLobbyIntent): void {
  intent = next
}

/** Read and clear. Undefined when nothing asked for anything in particular. */
export function takePendingLobbyIntent(): PendingLobbyIntent | undefined {
  const current = intent
  intent = null
  return current ?? undefined
}

export function setPendingLobbyApply(next: PendingLobbyApply): void {
  apply = next
}

/**
 * Read and clear the queued apply. Called by the first lobby broadcast after a create.
 *
 * The caller sends the messages, because sending is the store's job and this module deliberately
 * knows nothing about the socket.
 */
export function takePendingLobbyApply(): PendingLobbyApply | null {
  const current = apply
  apply = null
  return current
}

/** Drop anything queued — a create that never landed shouldn't reconfigure the *next* lobby. */
export function clearPendingLobby(): void {
  intent = null
  apply = null
}
