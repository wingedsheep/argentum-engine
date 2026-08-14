/**
 * Turn a {@link LobbyRecipe} into a lobby.
 *
 * The one path from "a game I want to play" to a live lobby, used by the landing wizard, a saved
 * setup chip, a rematch, and the cross-kind recreate. Before it there were three hand-written
 * variants of this sequence — `HomeScreen.launch`, `useLobbyCommands.recreate` and the wizard's own
 * AI-seat loop — and all three hardcoded `['ECL'], 6, 45, false` because there was nothing else to
 * send.
 *
 * ## The order, and why each step is where it is
 *
 * 1. **Leave.** `QuickGameLobbyHandler.handleCreate` rejects outright with "Already in a lobby", and
 *    while the tournament create *does* call `leaveCurrentLobbyIfPresent`, that only knows about
 *    tournament lobbies. Messages go out over one socket in order, so leave-then-create is enough.
 * 2. **Hand the intent across.** The lobby screen has not mounted; see `pendingLobbyIntent.ts`.
 * 3. **Create with the recipe's own values.** Not a placeholder: the first server broadcast is then
 *    already close to right, instead of painting a set nobody chose and re-painting a moment later.
 * 4. **Queue the rest**, flushed by that first broadcast — one omnibus message, cube first.
 * 5. **AI seats last**, at flush time: switching `gameMode` with AI already seated is rejected.
 *
 * Steps 4 and 5 are the store's; this hook only decides *what* and hands it over.
 */
import { useCallback } from 'react'
import { useGameStore } from '@/store/gameStore'
import type { AiDeckSpec, AvailableSet } from '@/types'
import {
  setPendingLobbyApply,
  setPendingLobbyIntent,
  type PendingCubeUpdate,
  type PendingSettingsUpdate,
} from '@/store/slices/pendingLobbyIntent'
import { resolveLaunch, seatCap } from './modeMatrix'
import type { LobbyRecipe, RecipeSettings } from './lobbyRecipe'
import type { DeckPickerTab } from '../ui/DeckPicker'

/**
 * Last-resort set code, for a create that happens before the catalogue has arrived.
 *
 * The server requires at least one set code on `createTournamentLobby` and rejects the whole create
 * without one (`LobbyHandler:597`), so a lobby always has to be born holding *something*.
 * {@link defaultSetCode} normally picks a real one; this is only the fallback for the first seconds
 * of a connection, and for the cross-kind recreate, which has no recipe to consult.
 */
export const BOOTSTRAP_SET_CODE = 'ECL'

/**
 * The set a lobby opens on when the recipe names none: Lorwyn Eclipsed.
 *
 * The alternative considered — and rejected — was opening with *no* sets at all, so that nothing
 * is chosen on the host's behalf. It is more honest about what has been decided, but it is a
 * regression on the thing this whole change is for: every wizard-made draft lobby would need a set
 * picked before Start could be pressed, and the host has to open the picker whatever they want.
 *
 * Prefer the catalogue entry rather than blindly returning the bootstrap code, so an unusual server
 * catalogue can still provide a usable complete, non-extension set. Extension sets can't carry a
 * pool alone (`startBlockReason` rejects an extension-only selection), and partial sets have a
 * knowingly incomplete pool.
 */
function defaultSetCode(availableSets: readonly AvailableSet[]): string {
  return availableSets.find((s) => s.code === BOOTSTRAP_SET_CODE)?.code
    ?? availableSets.find((s) => !s.extensionSet && !s.partial)?.code
    ?? BOOTSTRAP_SET_CODE
}

/**
 * The `boosterCount` that means "you choose" on a create message.
 *
 * Not a pack count — a sentinel. `LobbyHandler:664` reads a literal 6 as "use the format default"
 * and only honours other values, so 3 packs is what a draft lobby is actually born with.
 */
const BOOSTER_COUNT_SERVER_DEFAULT = 6

export function useApplyRecipe(): (recipe: LobbyRecipe, notes?: readonly string[]) => void {
  // Read lazily rather than subscribing: this is a write path, and a bare `useGameStore()` would
  // re-render the caller on every unrelated store change.
  const store = useGameStore.getState

  return useCallback((recipe: LobbyRecipe, notes: readonly string[] = []) => {
    const s = store()
    const spec = resolveLaunch(recipe.selection)
    const settings = recipe.settings

    if (s.quickGameLobbyState) s.leaveQuickGameLobby()
    else if (s.lobbyState) s.leaveLobby()

    setPendingLobbyIntent({
      deckTab: deckTabFor(recipe),
      ...(recipe.deck.kind === 'SAVED' ? { deckName: recipe.deck.name } : {}),
      autoStart: recipe.autoStart,
      ...(notes.length > 0 ? { notes: [...notes] } : {}),
    })

    if (spec.kind === 'QUICK') {
      // Everything a quick lobby has is a create-message field except the AI's deck, which is keyed
      // on the lobby existing — so that one waits for the broadcast and the rest ride the create.
      s.createQuickGameLobby(
        spec.vsAi,
        undefined,
        settings.isPublic ?? false,
        settings.deckFormat ?? undefined,
        spec.momirBasic,
        settings.ranked ?? false,
      )
      const aiDeck = aiDeckSpecFor(recipe)
      setPendingLobbyApply({ ...(aiDeck ? { aiDeck } : {}), aiSeats: 0 })
      return
    }

    const setCodes = settings.setCodes && settings.setCodes.length > 0
      ? [...settings.setCodes]
      : [defaultSetCode(s.availableSets)]
    s.createTournamentLobby(
      setCodes,
      spec.format,
      // `boosterCount` is a *sentinel* on the create message, not a value: 6 means "pick the
      // format's default" (3 packs for a draft, 8 for Commander Sealed — `LobbyHandler:664`). A
      // recipe that really wants six packs therefore cannot say so here, and says it in the bag
      // below instead, where the same field is read literally.
      BOOSTER_COUNT_SERVER_DEFAULT,
      seatCap(recipe.selection.roster, recipe.selection.cards, recipe.selection.shape),
      settings.pickTimeSeconds ?? 45,
      settings.isPublic ?? false,
      spec.gameMode,
      settings.rules ?? spec.rules,
    )

    const cube = cubeUpdateFor(settings)
    setPendingLobbyApply({
      ...(cube ? { cube } : {}),
      settings: tournamentSettings(settings),
      aiSeats: recipe.aiSeats,
    })
  }, [store])
}

/**
 * Which tab the deck picker opens on.
 *
 * Random pool *is* the Random tab — it is not a lobby flag — so a recipe that promises one has to
 * move the picker. See `pendingLobbyIntent.ts`.
 */
function deckTabFor(recipe: LobbyRecipe): DeckPickerTab {
  if (recipe.deck.kind === 'RANDOM') return 'random'
  return 'saved'
}

function aiDeckSpecFor(recipe: LobbyRecipe): AiDeckSpec | null {
  if (!recipe.aiDeck) return null
  return recipe.aiDeck.kind === 'SETS'
    ? { type: 'sets', setCodes: [...recipe.aiDeck.setCodes] }
    : { type: 'auto' }
}

function cubeUpdateFor(settings: RecipeSettings): PendingCubeUpdate | null {
  const cube = settings.cube
  if (!cube) return null
  return {
    cubeCards: [...cube.cards],
    cubeName: cube.name,
    cubeBasicLandSetCode: cube.basicLandSetCode,
    packSize: cube.packSize,
    cubePoolPlay: cube.poolPlay,
  }
}

/**
 * Everything the create message could not carry.
 *
 * `setCodes`, `format`, `pickTimeSeconds`, `isPublic`, `gameMode` and `rules` are create-message
 * fields already; re-sending `format` in particular would be actively wrong, because applying it a
 * second time resets `boosterCount`, `picksPerRound` and `chaosBoosters` and recalculates the booster
 * distribution. So the bag is deliberately the complement, not the whole recipe.
 *
 * The exceptions are the two fields the create message reads as something other than themselves:
 * `boosterCount` (a sentinel there, a value here) and `picksPerRound` (defaulted to 2 for the two
 * draft formats at create). Both are restated so a captured count survives the trip.
 */
function tournamentSettings(settings: RecipeSettings): PendingSettingsUpdate {
  return {
    ...(settings.boosterCount !== undefined ? { boosterCount: settings.boosterCount } : {}),
    ...(settings.boosterDistribution ? { boosterDistribution: { ...settings.boosterDistribution } } : {}),
    ...(settings.chaosBoosters !== undefined ? { chaosBoosters: settings.chaosBoosters } : {}),
    ...(settings.includedSetProducts ? { includedSetProducts: settings.includedSetProducts } : {}),
    ...(settings.bannedCardNames ? { bannedCardNames: [...settings.bannedCardNames] } : {}),
    ...(settings.picksPerRound !== undefined ? { picksPerRound: settings.picksPerRound } : {}),
    ...(settings.gamesPerMatch !== undefined ? { gamesPerMatch: settings.gamesPerMatch } : {}),
    ...(settings.deckFormat !== undefined ? { deckFormat: settings.deckFormat ?? '' } : {}),
    ...(settings.deckSizeMin !== undefined ? { deckSizeMin: settings.deckSizeMin } : {}),
    ...(settings.allowDuplicates !== undefined ? { allowDuplicates: settings.allowDuplicates } : {}),
    ...(settings.commanderPreset ? { commanderPreset: settings.commanderPreset } : {}),
    ...(settings.attackMode ? { attackMode: settings.attackMode } : {}),
    ...(settings.randomTeams !== undefined ? { randomTeams: settings.randomTeams } : {}),
    ...(settings.aiAssistEnabled !== undefined ? { aiAssistEnabled: settings.aiAssistEnabled } : {}),
  }
}
