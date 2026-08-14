import { AbilityFlag, Color, CounterType, DayNight, Keyword, Phase, Step, ZoneType } from './enums'
import { EntityId, ZoneId } from './entities'
import { ClientEvent } from './events'

/**
 * Client-facing game state DTO.
 * Matches backend ClientGameState.kt
 */
export interface ClientGameState {
  /** The player viewing this state */
  readonly viewingPlayerId: EntityId

  /** All visible cards/permanents */
  readonly cards: Record<EntityId, ClientCard>

  /** Zone information */
  readonly zones: readonly ClientZone[]

  /** Player information */
  readonly players: readonly ClientPlayer[]

  /** Current phase and step */
  readonly currentPhase: Phase
  readonly currentStep: Step

  /** Whose turn it is */
  readonly activePlayerId: EntityId

  /** Who currently has priority */
  readonly priorityPlayerId: EntityId

  /** Turn number */
  readonly turnNumber: number

  /** Whether the game is over */
  readonly isGameOver: boolean

  /** The winner, if the game is over */
  readonly winnerId: EntityId | null

  /** Combat state, if in combat */
  readonly combat: ClientCombatState | null

  /** Accumulated game log entries from the server */
  readonly gameLog?: readonly ClientEvent[]

  /**
   * Whether the global Void condition is satisfied this turn (a nonland permanent left
   * the battlefield this turn or a spell was warped this turn). Drives UI cues for cards
   * with Void abilities (Edge of Eternities).
   */
  readonly voidActive?: boolean

  /**
   * The game's day/night designation (Innistrad, CR 731), or absent/null while it's neither — the
   * state the game starts in and never returns to once a designation is gained. Public information,
   * so never masked. Drives the day/night indicator. See {@link DayNight}.
   */
  readonly dayNight?: DayNight | null

  /**
   * If non-null, the affected player whose turn the viewing player is currently driving
   * (Mindslaver-style hijack). Drives the controller banner and promoting their hand to
   * face-up. Null in normal play.
   */
  readonly youAreHijacking?: EntityId | null

  /**
   * If non-null, the controller currently driving the viewing player's turn. Drives the
   * affected-player banner and disabling click handlers. Null in normal play.
   */
  readonly youAreHijackedBy?: EntityId | null

  /**
   * True when this client controls every seat for the whole game — single-client hotseat
   * (play against yourself). Drives the "controlling both players" banner and lets the
   * client act for whichever seat currently has priority. Never set together with
   * {@link youAreHijacking}.
   */
  readonly hotseat?: boolean

  /**
   * The viewing player's active persistent yields (MTGO right-click yields — backlog §C).
   * Masked per-player: only your own yields appear. Drives the Active Yields panel.
   */
  readonly activeYields?: readonly ClientYield[]

  /**
   * The viewing player's own decklist — one row per distinct card, with how many copies they
   * still haven't seen. Drives the in-game deck tracker. Always empty for spectators, and never
   * describes anyone but the viewer, so opening it reveals nothing about an opponent's deck.
   */
  readonly deck?: readonly ClientDeckCard[]
}

/**
 * One distinct card in the viewing player's deck. Structurally a superset of the recorded-deck
 * `GameDeckCard` the profile/admin deck viewer uses, so both render through `DeckCardBody`.
 */
export interface ClientDeckCard {
  readonly cardName: string
  /** Total copies in the deck (all zones; the sideboard is not counted). */
  readonly copies: number
  /**
   * Copies whose location the viewer can't identify — still in the library, or hidden from them
   * elsewhere (e.g. a card of theirs exiled face down). In an ordinary game this is exactly
   * "copies left to draw"; the fuzzier definition is what stops it leaking face-down exiles.
   */
  readonly remaining: number
  /** Mana value, for the curve histogram. */
  readonly cmc: number
  /** Card type enum names, e.g. `["CREATURE"]`. */
  readonly cardTypes: string[]
  /** The card's own colours as enum names; empty for colourless. */
  readonly colors: string[]
  /** Art URL for the hover preview; null falls back to a name lookup. */
  readonly imageUri: string | null
}

/** The stable (cardDefinitionId, abilityId) key an ability is yielded against. */
export interface ClientAbilityIdentity {
  readonly cardDefinitionId: string
  readonly abilityId: string
}

/** One ability the viewing player has set a yield on (flattened from PlayerYields). */
export interface ClientYield {
  readonly cardDefinitionId: string
  readonly abilityId: string
  readonly displayName: string
  readonly untilEndOfTurn?: boolean
  readonly wholeGame?: boolean
  /** true = always yes, false = always no, null/absent = no auto-answer. */
  readonly autoAnswer?: boolean | null
}

/**
 * Card/permanent information for client display.
 * Matches backend ClientCard.kt
 */
export interface ClientCard {
  /** Unique identifier */
  readonly id: EntityId

  /** Card name for display */
  readonly name: string

  /** Mana cost as a string (e.g., "{2}{G}{G}") */
  readonly manaCost: string

  /** Converted mana cost / mana value */
  readonly manaValue: number

  /** Type line as displayed on the card (e.g., "Creature - Human Warrior") */
  readonly typeLine: string

  /** Card types for filtering (creature, land, instant, etc.) */
  readonly cardTypes: readonly string[]

  /** Subtypes for display and filtering (e.g., "Human", "Warrior", "Forest") */
  readonly subtypes: readonly string[]

  /** Card colors */
  readonly colors: readonly Color[]

  /**
   * Colors this permanent has been granted by an attached "choose a color" aura
   * (e.g. Shimmerwilds Growth's "Enchanted land is the chosen color"). The chosen
   * color lives on the hidden aura behind the host, so it's surfaced here to paint
   * a mana pip on the host showing the color it has become.
   */
  readonly grantedColors?: readonly Color[]

  /** Oracle text / rules text (for display in card details) */
  readonly oracleText: string

  /** Power for creatures (null if not a creature) - projected/modified value */
  readonly power: number | null

  /** Toughness for creatures (null if not a creature) - projected/modified value */
  readonly toughness: number | null

  /** Base power before modifications (for buff indicators) */
  readonly basePower: number | null

  /** Base toughness before modifications (for buff indicators) */
  readonly baseToughness: number | null

  /** Current damage on creature (only present on battlefield) */
  readonly damage: number | null

  /** Keywords the card has (flying, haste, etc.) */
  readonly keywords: readonly Keyword[]

  /** Ability flags (non-keyword static abilities like "can't be blocked") */
  readonly abilityFlags?: readonly AbilityFlag[]

  /** Protection colors (for colored protection shield icons) */
  readonly protections?: readonly Color[]

  /** Hexproof-from-color colors (for colored hexproof shield icons) */
  readonly hexproofFromColors?: readonly Color[]

  /** Hexproof from monocolored (CR 105.2) — shows an uncolored hexproof shield chip */
  readonly hexproofFromMonocolored?: boolean

  /** Counters on the card */
  readonly counters: Partial<Record<CounterType, number>>

  /** State flags */
  readonly isTapped: boolean
  /** Exerted (CR 701.43a) — won't untap during its controller's next untap step. */
  readonly isExerted?: boolean
  readonly hasSummoningSickness: boolean
  readonly isTransformed: boolean
  /** Phased out (Rule 702.26) — treated as though it doesn't exist; rendered translucent. */
  readonly isPhasedOut?: boolean

  /** True when this card is a double-faced card (DFC). */
  readonly isDoubleFaced?: boolean
  /** For DFCs currently on the battlefield: 'FRONT' or 'BACK'. Null otherwise. */
  readonly currentFace?: 'FRONT' | 'BACK' | null
  /** Back face display name for DFCs. */
  readonly backFaceName?: string | null
  /** Back face type line for DFCs. */
  readonly backFaceTypeLine?: string | null
  /** Back face oracle text for DFCs. */
  readonly backFaceOracleText?: string | null
  /** Back face image URI for DFCs. */
  readonly backFaceImageUri?: string | null

  /** Combat state (if in combat) */
  readonly isAttacking: boolean
  readonly isBlocking: boolean
  readonly attackingTarget: EntityId | null
  readonly blockingTarget: EntityId | null

  /** Controller (who controls it now) */
  readonly controllerId: EntityId

  /**
   * For a battle (CR 310): the player designated as its protector — the one who defends it, may
   * never attack it, and is the only player who may block creatures attacking it. Usually *not*
   * the controller: a Siege is protected by an opponent of the player who cast it. Absent on every
   * non-battle permanent. The battle's defense is its `defense` counter count, not a field.
   */
  readonly protectorId?: EntityId | null

  /** Owner (who started with it in their deck) */
  readonly ownerId: EntityId

  /** Whether this is a token */
  readonly isToken: boolean

  /**
   * True when this card is a designated commander (Commander format). Set in every zone — hand,
   * stack, battlefield, command — so the UI can keep the crown / gold border on the card after
   * it's cast. Token copies of a commander never carry this (CR 903.10a).
   */
  readonly isCommander?: boolean

  /**
   * True when this permanent is its controller's Ring-bearer (CR 701.54). Battlefield only — the
   * designation is stripped on a real control change, so a stolen permanent / token copy never
   * carries it. Drives the prominent golden Ring badge on the card.
   */
  readonly isRingBearer?: boolean

  /**
   * The creature this one is soulbond-paired with (CR 702.95b), or absent while unpaired.
   * Battlefield only, and always symmetric — the server drops both halves together the moment the
   * pair breaks — so `SoulbondBonds` can dedupe on the id pair and draw one bond per pair.
   */
  readonly pairedWithId?: EntityId | null

  /** Zone this card is currently in */
  readonly zone: ZoneId | null

  /** Attached to (for auras, equipment) */
  readonly attachedTo: EntityId | null

  /** What's attached to this card (auras, equipment on this permanent) */
  readonly attachments: readonly EntityId[]

  /** Cards exiled by this permanent via linked exile (e.g., Suspension Field) */
  readonly linkedExile?: readonly EntityId[]

  /** Whether this card is face-down (for morph, manifest, hidden info) */
  readonly isFaceDown: boolean

  /**
   * Which mechanic made this permanent face down — 'MORPH' | 'MANIFEST' | 'DISGUISE' | 'CLOAK'.
   * Public information (CR 708.6); picks the face-down helper-card art.
   */
  readonly faceDownMode?: string

  /** Whether this permanent is suspected (CR 701.60 — has menace and can't block). Battlefield only. */
  readonly isSuspected?: boolean

  /** Whether this card is plotted in exile (CR 718 — Plot keyword, castable for free on a later turn). Exile only. */
  readonly isPlotted?: boolean

  /** Whether this card is an active paradigm card in exile (Secrets of Strixhaven — Paradigm): it stays
   * exiled and casts a free copy of itself each precombat main phase. Surfaced in a dedicated public
   * pile so both players can read it. Exile only. */
  readonly isParadigm?: boolean

  /** Whether this card is actively suspended in exile (CR 702.62 — has at least one time counter left).
   * Surfaced in a dedicated public pile so both players can read it. False once the last time counter
   * is removed, even if the card lingers in exile after the owner declines the free cast. Exile only. */
  readonly isSuspended?: boolean

  /** Whether this permanent is prepared (Secrets of Strixhaven — Prepared keyword): a copy of its
   * prepare spell sits castable in its controller's exile. Battlefield only. */
  readonly isPrepared?: boolean

  /** Whether this exiled card is the prepare-spell copy of a prepared permanent (Secrets of
   * Strixhaven). It appears as a castable ghost card in the controller's hand; drives the
   * "Prepared" badge that links it back to the prepared creature. Exile only. */
  readonly isPreparedSpell?: boolean

  /** Whether this permanent was cast for its warp cost (CR 702.185, Edge of Eternities): it will be
   * exiled at the next end step, then can be recast from exile. Drives the cosmic warp cue. Battlefield only. */
  readonly isWarped?: boolean

  /** Whether this permanent was cast for its dash cost (CR 702.109, Khans of Tarkir): it has haste
   * and will be returned to its owner's hand at the next end step (not exiled — unlike warp).
   * Battlefield only. */
  readonly isDashed?: boolean

  /** Morph cost for face-down creatures (only visible to controller) */
  readonly morphCost?: string | null

  /** Targets for spells/abilities on the stack (for targeting arrows) */
  readonly targets: readonly ClientChosenTarget[]

  /** Image URI from card metadata (for rendering card images) */
  readonly imageUri?: string | null

  /**
   * Clockwise rotation in degrees for the card art (default 0 / absent). Non-zero only for
   * flip-layout tokens whose single image shows the other face upright (e.g. the WOE "Cursed" /
   * "Sorcerer" Roles need 180). Purely cosmetic.
   */
  readonly imageRotation?: number

  /** Active effects on this card (e.g., "can't be blocked except by black creatures") */
  readonly activeEffects?: readonly ClientCardEffect[]

  /** Official rulings for this card (for card details view) */
  readonly rulings?: readonly ClientRuling[]

  /**
   * Name of the optional additional cost this spell declared — "Kicked", "Bargained", "Offspring"
   * — or absent when it declared none. Server-derived label; render verbatim (only on the stack).
   */
  readonly optionalCostLabel?: string

  /**
   * How this spell was cast — "Disturb · Graveyard", "Command zone" — or absent for an ordinary
   * cast from hand. Server-derived label; render verbatim (only on the stack).
   */
  readonly castProvenanceLabel?: string

  /**
   * What this spell's alternative cost consumed — "Sacrificed Niblis of the Urn" — or absent when it
   * consumed nothing. Server-derived label; render verbatim (only on the stack). Emerge
   * (CR 702.119a) needs it: the sacrifice is what made the spell cheap.
   */
  readonly costSacrificeLabel?: string

  /**
   * The mana actually spent on this cast ("{W}{W}{W}{U}"), or absent for a normal cast. Only sent for
   * alternative-cost casts, whose printed cost says nothing about what was paid (only on the stack).
   */
  readonly manaPaidCost?: string

  /** Whether this spell promised a gift (Bloomburrow gift mechanic — only present on stack) */
  readonly giftPromised?: boolean

  /** Whether this spell's optional Blight additional cost was paid (Lorwyn Eclipsed — only present on stack) */
  readonly wasBlightPaid?: boolean

  /** Chosen X value for spells with X in their cost (only present on stack) */
  readonly chosenX?: number | null

  /**
   * For a triggered/activated ability on the stack: its definition-scoped identity (backlog §C).
   * Drives the stack-item yield context menu. Absent for spells.
   */
  readonly abilityIdentity?: ClientAbilityIdentity | null

  /** Copy index for storm/copy effects on the stack (1, 2, 3...) */
  readonly copyIndex?: number | null

  /** Total number of copies for storm/copy effects on the stack */
  readonly copyTotal?: number | null

  /** Chosen creature type for "as enters, choose a creature type" permanents (e.g., Doom Cannon) */
  readonly chosenCreatureType?: string | null

  /** Chosen color for "as enters, choose a color" permanents (e.g., Riptide Replicator) */
  readonly chosenColor?: string | null

  /**
   * Chosen mode label for "as enters, choose X or Y" permanents (e.g., the Siege cycle).
   * Rendered as a badge on the permanent so the player can see which mode is active.
   */
  readonly chosenMode?: string | null

  /** Chosen card name for "as enters, choose a card name" permanents (e.g., Petrified Hamlet) */
  readonly chosenCardName?: string | null

  /** Chosen card type for "choose a card type" permanents (e.g., Arachne, Psionic Weaver) */
  readonly chosenCardType?: string | null

  /** Triggering entity ID for triggered abilities on the stack (for source arrows) */
  readonly triggeringEntityId?: EntityId | null

  /** Source zone for triggered abilities on the stack (e.g., "GRAVEYARD" for graveyard triggers) */
  readonly sourceZone?: string | null

  /** Creature types of the sacrificed permanent (for spells like Endemic Plague on the stack) */
  readonly sacrificedCreatureTypes?: readonly string[] | null

  /** Specific ability text when on the stack (e.g., spell effect description, not full oracle text) */
  readonly stackText?: string | null

  /**
   * Runtime descriptions of each chosen mode, in the order they were picked (rule 700.2).
   * Empty for non-modal spells and for modal spells whose mode hasn't been selected yet.
   * For opponent visibility of choose-N commands (Brigid's Command, Sygg's Command, etc.).
   */
  readonly chosenModeDescriptions?: readonly string[]

  /**
   * Per-mode target groups for modal spells on the stack, aligned 1:1 with `chosenModeDescriptions`.
   * Each group carries the mode description, chosen targets (for arrows), and human-readable target names.
   */
  readonly perModeTargets?: readonly ClientPerModeTargetGroup[]

  /** Revealed name for face-down creatures that this player has peeked at (e.g., via Spy Network) */
  readonly revealedName?: string | null

  /** Revealed image URI for face-down creatures that this player has peeked at */
  readonly revealedImageUri?: string | null

  /** Whether this card can be played from exile (e.g., Mind's Desire impulse draw) */
  readonly playableFromExile?: boolean

  /** Original card name when this permanent is a copy (e.g., "Clever Impersonator") */
  readonly copyOf?: string | null

  /**
   * True when the printed card is legendary but this permanent's projected type line is not —
   * a copy effect explicitly stripped legendariness ("except it isn't legendary" /
   * Impostor Syndrome). The UI badges this so a non-legendary token copy of a legendary
   * creature is visually distinguishable from the original. The server keeps this mutually
   * exclusive with {@link legendaryByEffect} — an effect that grants the supertype back wins,
   * because the permanent then really is legendary.
   */
  readonly nonLegendaryCopy?: boolean

  /**
   * The mirror of {@link nonLegendaryCopy}: the printed card is not legendary but a continuous
   * effect has granted the Legendary supertype (Origin of Spider-Man, the Ring emblem's
   * "your Ring-bearer is legendary"). The printed art shows a non-legendary frame, so the UI
   * badges it to make the legend rule visible.
   */
  readonly legendaryByEffect?: boolean

  /**
   * Subtypes granted by a continuous effect rather than printed on the card (Super-Soldier Serum's
   * "is a legendary Soldier in addition to its other types"). The battlefield shows the printed card
   * image and the preview only prints the type line for tokens, so without surfacing these the grant
   * is invisible. Empty when nothing was granted.
   */
  readonly grantedSubtypes?: readonly string[]

  /**
   * Card types granted by a continuous effect rather than printed (I Am Iron Man's "becomes an
   * artifact creature", a manland's animation). Uppercase, matching {@link cardTypes}. Invisible on
   * the battlefield for the same reason as {@link grantedSubtypes} — the printed image is drawn.
   */
  readonly grantedCardTypes?: readonly string[]

  /** Damage distribution for DividedDamageEffect spells on the stack (target entity ID -> damage amount) */
  readonly damageDistribution?: Record<EntityId, number> | null

  /** For Sagas: the total number of chapters (e.g., 3). Null for non-Sagas. */
  readonly sagaTotalChapters?: number | null

  /** For Class enchantments: the current class level (1, 2, or 3). Null for non-Classes. */
  readonly classLevel?: number | null

  /** For Class enchantments: the maximum class level (e.g., 3). Null for non-Classes. */
  readonly classMaxLevel?: number | null

  /**
   * Threshold-style progress: present on cards whose static ability turns on at a
   * graveyard-size milestone (e.g. classic Threshold = 7+). Lets the UI render a badge
   * showing current/required graveyard count for the card's controller.
   */
  readonly thresholdInfo?: {
    readonly current: number
    readonly required: number
    readonly active: boolean
  } | null

  /**
   * Delirium progress: present only on cards whose definition cares about delirium ("four or
   * more card types among cards in your graveyard"). Lets the UI render a badge showing the
   * distinct card-type count (`current`) out of the threshold (`required`, 4 in practice) for
   * the card's controller. Absent on cards that don't reference delirium.
   */
  readonly deliriumInfo?: {
    readonly current: number
    readonly required: number
    readonly active: boolean
  } | null

  /**
   * For planeswalkers on the battlefield: the complete set of loyalty abilities,
   * in declaration order. The UI renders the full list and grays out any ability
   * whose `abilityId` isn't present in the legal actions for this card.
   */
  readonly planeswalkerAbilities?: readonly ClientPlaneswalkerAbility[] | null

  /** True if this is a split-layout Room (CR 709.5). Drives split-card rendering + lock UI. */
  readonly isRoom?: boolean

  /**
   * True when the face currently shown is **printed sideways** — its image is a portrait file
   * holding a card on its side. Drives the 90° rotation and the landscape footprint wherever the
   * card is drawn: battlefield, stack, and hover previews.
   *
   * The server decides which cards qualify (`CardDefinition.isLandscapePrint` — split layouts
   * including Rooms, and battles). Renderers must read this rather than re-deriving orientation
   * from `isRoom` / `cardFaces` / type lines, which is how battles ended up rendering sideways.
   * Per *face*: a Siege reports true, and the portrait back face it becomes when defeated reports
   * false.
   */
  readonly isLandscapeFace?: boolean

  /**
   * {@link isLandscapeFace} for the card's *other* face — what a hover preview shows when flipped.
   * Flipping swaps the image in both directions, so peeking at a Siege's portrait back must not
   * rotate and peeking at the landscape front of an already-transformed permanent must.
   */
  readonly backFaceIsLandscape?: boolean

  /**
   * For split-layout cards (currently Rooms): one entry per face. `isUnlocked` reflects the live
   * door state on the battlefield; in other zones it's always false.
   */
  readonly cardFaces?: readonly ClientCardFace[]

  /** For Rooms on the stack: index into `cardFaces` of the face that was cast. */
  readonly castFaceIndex?: number | null

  /**
   * Impending alternative cost (CR 702.176), present iff the card definition has impending. Lets
   * the action menu always offer the impending cast option (reduced `cost`, enters with `time`
   * time counters) next to the normal cast, graying out whichever the player can't pay for and
   * annotating impending with a time-counter glyph.
   */
  readonly impending?: {
    /** Reduced mana cost to cast for impending, e.g. "{2}{W}{W}". */
    readonly cost: string
    /** Number of time counters the permanent enters with (e.g. 4). */
    readonly time: number
  } | null
}

/** One face of a split-layout card (CR 709). */
export interface ClientCardFace {
  /** Stable face id — currently the face's printed name. */
  readonly faceId: string
  readonly name: string
  readonly manaCost: string
  readonly typeLine: string
  readonly oracleText: string
  /** Door state on the battlefield; always false in other zones. */
  readonly isUnlocked: boolean
}

/** One loyalty ability on a planeswalker (always-visible menu rendering). */
export interface ClientPlaneswalkerAbility {
  /** Matches `ActivateAbility.abilityId` in legal actions. */
  readonly abilityId: string
  /** Signed loyalty change (+1, -2, -8, etc.). */
  readonly loyaltyChange: number
  /** Ability text. */
  readonly description: string
}

/**
 * Zone information for client display.
 * Matches backend ClientZone.kt
 */
export interface ClientZone {
  readonly zoneId: ZoneId

  /** Card IDs in this zone, in order (may be empty for hidden zones) */
  readonly cardIds: readonly EntityId[]

  /** Number of cards in the zone (always available, even for hidden zones) */
  readonly size: number

  /** Whether the contents are visible to the viewing player */
  readonly isVisible: boolean
}

/**
 * Player information for client display.
 * Matches backend ClientPlayer.kt
 */
export interface ClientPlayer {
  readonly playerId: EntityId
  readonly name: string
  readonly life: number
  readonly poisonCounters: number
  readonly handSize: number
  /**
   * Effective maximum hand size (CR 402.2): 7 by default, a different number when an effect set it
   * (Cursed Rack), or `null` when the player has no maximum (Reliquary Tower). The UI surfaces a
   * badge only when this differs from the default 7 (or is unlimited).
   */
  readonly maxHandSize?: number | null
  readonly librarySize: number
  readonly graveyardSize: number
  readonly exileSize: number
  readonly landsPlayedThisTurn: number
  readonly hasLost: boolean
  readonly manaPool?: ClientManaPool
  readonly activeEffects?: readonly ClientPlayerEffect[]
  /**
   * Per-commander combat damage dealt to this player (CR 903.10a). Empty outside Commander format.
   * Rendered as a progress badge under the life orb.
   */
  readonly commanderDamage?: readonly ClientCommanderDamage[]
  /**
   * This player's speed, 0–4 (Aetherdrift, CR 702.179). `0` means they have no speed and no gauge is
   * rendered; `4` is max speed, which switches on every "Max speed —" ability they control.
   */
  readonly speed?: number
  /**
   * This player's current energy counter total (Kaladesh block onward, CR 107.14). `0` means no
   * badge is rendered.
   */
  readonly energyCounters?: number
}

/**
 * Per-commander commander-damage tally against this player. Matches backend
 * ClientCommanderDamage.kt.
 */
export interface ClientCommanderDamage {
  readonly commanderId: EntityId
  readonly commanderName: string
  readonly controllerId: EntityId
  readonly amount: number
  readonly threshold: number
  readonly imageUri?: string
}

/**
 * An active effect on a player that should be displayed as a badge.
 * Matches backend ClientPlayerEffect.kt
 */
export interface ClientPlayerEffect {
  /** Unique identifier for the effect type */
  readonly effectId: string
  /** Human-readable name for display */
  readonly name: string
  /** Optional description/tooltip text */
  readonly description?: string
  /** Optional icon identifier for UI rendering */
  readonly icon?: string
  /**
   * Optional image URL — typically a Scryfall marker-card image (e.g. the
   * "City's Blessing" marker). When present, the badge renders this image
   * in place of the emoji icon.
   */
  readonly imageUri?: string
  /**
   * Optional progression for cumulative effects that climb toward a cap
   * (e.g. The Ring's four temptations). Rendered as filled/empty pips.
   */
  readonly progress?: ClientEffectProgress
}

/**
 * A staged progression for a cumulative player effect: `current` steps reached
 * out of `total` meaningful steps. `current` may exceed `total`.
 * Matches backend ClientEffectProgress.
 */
export interface ClientEffectProgress {
  readonly current: number
  readonly total: number
}

/**
 * An active effect on a card that should be displayed as a badge.
 * Matches backend ClientCardEffect.kt
 */
export interface ClientCardEffect {
  /** Unique identifier for the effect type */
  readonly effectId: string
  /** Human-readable name for display */
  readonly name: string
  /** Optional description/tooltip text */
  readonly description?: string
  /** Optional icon identifier for UI rendering */
  readonly icon?: string
}

/**
 * An official ruling for a card.
 * Displayed in card details view to clarify complex interactions.
 * Matches backend ClientRuling.kt
 */
export interface ClientRuling {
  /** Date of the ruling (e.g., "6/8/2016") */
  readonly date: string
  /** The ruling text */
  readonly text: string
}

/**
 * Mana pool state for client display.
 * Matches backend ClientManaPool.kt
 */
export interface ClientManaPool {
  readonly white: number
  readonly blue: number
  readonly black: number
  readonly red: number
  readonly green: number
  readonly colorless: number
  readonly restrictedMana: ReadonlyArray<ClientRestrictedManaEntry>
}

/**
 * A single unit of restricted mana for client display.
 * Matches backend ClientRestrictedManaEntry.kt.
 */
export interface ClientRestrictedManaEntry {
  /** Mana color symbol ("W"/"U"/"B"/"R"/"G") or null for colorless. */
  readonly color: string | null
  /** Human-readable restriction (e.g., "Spend this mana only to cast spells with mana value 4 or greater"). */
  readonly restrictionDescription: string
}

/**
 * Calculate total mana in pool.
 */
export function totalMana(pool: ClientManaPool): number {
  return pool.white + pool.blue + pool.black + pool.red + pool.green + pool.colorless +
    (pool.restrictedMana?.length ?? 0)
}

/**
 * Check if mana pool is empty.
 */
export function isManaPoolEmpty(pool: ClientManaPool): boolean {
  return totalMana(pool) === 0
}

/**
 * Combat state for client display.
 * Matches backend ClientCombatState.kt
 */
export interface ClientCombatState {
  /** Who is attacking */
  readonly attackingPlayerId: EntityId

  /** Who is defending */
  readonly defendingPlayerId: EntityId

  /** All declared attackers with their targets */
  readonly attackers: readonly ClientAttacker[]

  /** All declared blockers with what they're blocking */
  readonly blockers: readonly ClientBlocker[]
}

/**
 * Attacker information for combat display.
 * Matches backend ClientAttacker.kt
 */
export interface ClientAttacker {
  readonly creatureId: EntityId
  readonly creatureName: string
  readonly attackingTarget: ClientCombatTarget
  readonly blockedBy: readonly EntityId[]
  /** True if all creatures that can block this creature must do so (Alluring Scent) */
  readonly mustBeBlockedByAll?: boolean
  /** Banding band id (CR 702.22) shared by every attacker in the same band; null/absent if not banded. */
  readonly bandId?: string | null
  /** Ordered list of blockers for damage assignment (first receives damage first). Null if not yet ordered. */
  readonly damageAssignmentOrder?: readonly EntityId[]
  /** Damage assigned to each target (blocker ID or player ID -> damage amount). Null if not yet assigned. */
  readonly damageAssignments?: Readonly<Record<EntityId, number>>
}

/**
 * What an attacker is attacking.
 * Matches backend ClientCombatTarget.kt
 */
export type ClientCombatTarget =
  | { readonly type: 'Player'; readonly playerId: EntityId }
  | { readonly type: 'Planeswalker'; readonly permanentId: EntityId }

/**
 * Blocker information for combat display.
 * Matches backend ClientBlocker.kt
 */
export interface ClientBlocker {
  readonly creatureId: EntityId
  readonly creatureName: string
  readonly blockingAttacker: EntityId
}

/**
 * Represents a chosen target for a spell or ability on the stack.
 * Matches backend ClientChosenTarget.kt
 */
export type ClientChosenTarget =
  | { readonly type: 'Player'; readonly playerId: EntityId }
  | { readonly type: 'Permanent'; readonly entityId: EntityId }
  | { readonly type: 'Spell'; readonly spellEntityId: EntityId }
  | { readonly type: 'Card'; readonly cardId: EntityId }

/**
 * A group of targets chosen for a single mode of a modal spell on the stack. The index
 * refers to the position within `ClientCard.perModeTargets`, not the original `Mode` index
 * (the same mode can appear twice with `allowRepeat`).
 * Matches backend ClientPerModeTargetGroup.kt
 */
export interface ClientPerModeTargetGroup {
  /** The mode index in the spell's `ModalEffect.modes` list. */
  readonly modeIndex: number
  /** Runtime description of the mode, with dynamic amounts evaluated. */
  readonly modeDescription: string
  /** Chosen targets for this mode, for arrow rendering. */
  readonly targets: readonly ClientChosenTarget[]
  /**
   * Human-readable target names aligned 1:1 with `targets`. For hidden-zone targets, the name
   * is generic ("a card in Opponent's hand") to avoid leaking hidden information.
   */
  readonly targetNames: readonly string[]
}

/**
 * Helper to check if a card is a creature.
 */
export function isCreature(card: ClientCard): boolean {
  return card.cardTypes.includes('Creature')
}

/**
 * Helper to check if a card is a land.
 */
export function isLand(card: ClientCard): boolean {
  return card.cardTypes.includes('Land')
}

/**
 * Helper to check if a card is an instant.
 */
export function isInstant(card: ClientCard): boolean {
  return card.cardTypes.includes('Instant')
}

/**
 * Helper to check if a card is a sorcery.
 */
export function isSorcery(card: ClientCard): boolean {
  return card.cardTypes.includes('Sorcery')
}

/**
 * Helper to get the effective toughness after damage.
 */
export function remainingToughness(card: ClientCard): number | null {
  if (card.toughness === null) return null
  return card.toughness - (card.damage ?? 0)
}

/**
 * Find a zone by type in the game state.
 */
export function findZone(
  state: ClientGameState,
  zoneType: ZoneType,
  ownerId: EntityId
): ClientZone | undefined {
  return state.zones.find(
    (z) => z.zoneId.zoneType === zoneType && z.zoneId.ownerId === ownerId
  )
}

/**
 * Get the viewing player's data.
 */
export function getViewingPlayer(state: ClientGameState): ClientPlayer | undefined {
  return state.players.find((p) => p.playerId === state.viewingPlayerId)
}

/**
 * Get the opponent's data.
 */
export function getOpponent(state: ClientGameState): ClientPlayer | undefined {
  return state.players.find((p) => p.playerId !== state.viewingPlayerId)
}

/**
 * Check if it's the viewing player's turn.
 */
export function isMyTurn(state: ClientGameState): boolean {
  return state.activePlayerId === state.viewingPlayerId
}

/**
 * Check if the viewing player has priority.
 */
export function hasPriority(state: ClientGameState): boolean {
  return state.priorityPlayerId === state.viewingPlayerId
}
