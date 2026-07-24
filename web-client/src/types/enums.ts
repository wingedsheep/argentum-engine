/**
 * Game phase enum matching backend Phase.kt
 */
export enum Phase {
  BEGINNING = 'BEGINNING',
  PRECOMBAT_MAIN = 'PRECOMBAT_MAIN',
  COMBAT = 'COMBAT',
  POSTCOMBAT_MAIN = 'POSTCOMBAT_MAIN',
  ENDING = 'ENDING',
}

export const PhaseDisplayNames: Record<Phase, string> = {
  [Phase.BEGINNING]: 'Beginning Phase',
  [Phase.PRECOMBAT_MAIN]: 'Precombat Main Phase',
  [Phase.COMBAT]: 'Combat Phase',
  [Phase.POSTCOMBAT_MAIN]: 'Postcombat Main Phase',
  [Phase.ENDING]: 'Ending Phase',
}

/**
 * Game step enum matching backend Step.kt
 */
export enum Step {
  // Beginning Phase
  UNTAP = 'UNTAP',
  UPKEEP = 'UPKEEP',
  DRAW = 'DRAW',
  // Precombat Main Phase
  PRECOMBAT_MAIN = 'PRECOMBAT_MAIN',
  // Combat Phase
  BEGIN_COMBAT = 'BEGIN_COMBAT',
  DECLARE_ATTACKERS = 'DECLARE_ATTACKERS',
  DECLARE_BLOCKERS = 'DECLARE_BLOCKERS',
  FIRST_STRIKE_COMBAT_DAMAGE = 'FIRST_STRIKE_COMBAT_DAMAGE',
  COMBAT_DAMAGE = 'COMBAT_DAMAGE',
  END_COMBAT = 'END_COMBAT',
  // Postcombat Main Phase
  POSTCOMBAT_MAIN = 'POSTCOMBAT_MAIN',
  // Ending Phase
  END = 'END',
  CLEANUP = 'CLEANUP',
}

export const StepDisplayNames: Record<Step, string> = {
  [Step.UNTAP]: 'Untap Step',
  [Step.UPKEEP]: 'Upkeep Step',
  [Step.DRAW]: 'Draw Step',
  [Step.PRECOMBAT_MAIN]: 'Precombat Main Phase',
  [Step.BEGIN_COMBAT]: 'Beginning of Combat Step',
  [Step.DECLARE_ATTACKERS]: 'Declare Attackers Step',
  [Step.DECLARE_BLOCKERS]: 'Declare Blockers Step',
  [Step.FIRST_STRIKE_COMBAT_DAMAGE]: 'First Strike Combat Damage Step',
  [Step.COMBAT_DAMAGE]: 'Combat Damage Step',
  [Step.END_COMBAT]: 'End of Combat Step',
  [Step.POSTCOMBAT_MAIN]: 'Postcombat Main Phase',
  [Step.END]: 'End Step',
  [Step.CLEANUP]: 'Cleanup Step',
}

/**
 * Short step names for compact UI elements like the pass button.
 */
export const StepShortNames: Record<Step, string> = {
  [Step.UNTAP]: 'Untap',
  [Step.UPKEEP]: 'Upkeep',
  [Step.DRAW]: 'Draw',
  [Step.PRECOMBAT_MAIN]: 'Main 1',
  [Step.BEGIN_COMBAT]: 'Combat',
  [Step.DECLARE_ATTACKERS]: 'Attackers',
  [Step.DECLARE_BLOCKERS]: 'Blockers',
  [Step.FIRST_STRIKE_COMBAT_DAMAGE]: 'First Strike',
  [Step.COMBAT_DAMAGE]: 'Damage',
  [Step.END_COMBAT]: 'End Combat',
  [Step.POSTCOMBAT_MAIN]: 'Main 2',
  [Step.END]: 'End Step',
  [Step.CLEANUP]: 'Cleanup',
}

/**
 * Get the next step after the given step.
 * Returns null if we're at cleanup (which would go to opponent's turn).
 */
export function getNextStep(currentStep: Step): Step | null {
  const stepOrder: Step[] = [
    Step.UNTAP,
    Step.UPKEEP,
    Step.DRAW,
    Step.PRECOMBAT_MAIN,
    Step.BEGIN_COMBAT,
    Step.DECLARE_ATTACKERS,
    Step.DECLARE_BLOCKERS,
    Step.FIRST_STRIKE_COMBAT_DAMAGE,
    Step.COMBAT_DAMAGE,
    Step.END_COMBAT,
    Step.POSTCOMBAT_MAIN,
    Step.END,
    Step.CLEANUP,
  ]

  const currentIndex = stepOrder.indexOf(currentStep)
  if (currentIndex === -1 || currentIndex === stepOrder.length - 1) {
    return null
  }
  return stepOrder[currentIndex + 1] ?? null
}

export function isMainPhaseStep(step: Step): boolean {
  return step === Step.PRECOMBAT_MAIN || step === Step.POSTCOMBAT_MAIN
}

/**
 * Mana color enum matching backend Color.kt
 */
export enum Color {
  WHITE = 'WHITE',
  BLUE = 'BLUE',
  BLACK = 'BLACK',
  RED = 'RED',
  GREEN = 'GREEN',
}

export const ColorSymbols: Record<Color, string> = {
  [Color.WHITE]: 'W',
  [Color.BLUE]: 'U',
  [Color.BLACK]: 'B',
  [Color.RED]: 'R',
  [Color.GREEN]: 'G',
}

export const ColorDisplayNames: Record<Color, string> = {
  [Color.WHITE]: 'White',
  [Color.BLUE]: 'Blue',
  [Color.BLACK]: 'Black',
  [Color.RED]: 'Red',
  [Color.GREEN]: 'Green',
}

/**
 * Keyword ability enum matching backend Keyword.kt
 */
export enum Keyword {
  // Evasion
  FLYING = 'FLYING',
  MENACE = 'MENACE',
  INTIMIDATE = 'INTIMIDATE',
  FEAR = 'FEAR',
  SHADOW = 'SHADOW',
  HORSEMANSHIP = 'HORSEMANSHIP',
  // Landwalk
  SWAMPWALK = 'SWAMPWALK',
  FORESTWALK = 'FORESTWALK',
  ISLANDWALK = 'ISLANDWALK',
  MOUNTAINWALK = 'MOUNTAINWALK',
  PLAINSWALK = 'PLAINSWALK',
  DESERTWALK = 'DESERTWALK',
  NONBASIC_LANDWALK = 'NONBASIC_LANDWALK',
  // Combat
  FIRST_STRIKE = 'FIRST_STRIKE',
  DOUBLE_STRIKE = 'DOUBLE_STRIKE',
  TRAMPLE = 'TRAMPLE',
  DEATHTOUCH = 'DEATHTOUCH',
  LIFELINK = 'LIFELINK',
  VIGILANCE = 'VIGILANCE',
  REACH = 'REACH',
  PROVOKE = 'PROVOKE',
  FLANKING = 'FLANKING',
  BANDING = 'BANDING',
  // ETB modification
  AMPLIFY = 'AMPLIFY',
  // Defense
  DEFENDER = 'DEFENDER',
  INDESTRUCTIBLE = 'INDESTRUCTIBLE',
  HEXPROOF = 'HEXPROOF',
  SHROUD = 'SHROUD',
  WARD = 'WARD',
  PROTECTION = 'PROTECTION',
  // Speed
  HASTE = 'HASTE',
  FLASH = 'FLASH',
  // Triggered/Static keyword abilities
  PROWESS = 'PROWESS',
  FLURRY = 'FLURRY',
  CHANGELING = 'CHANGELING',
  CRAFT = 'CRAFT',
  // Cost reduction
  CONVOKE = 'CONVOKE',
  DELVE = 'DELVE',
  AFFINITY = 'AFFINITY',
  // Spell mechanics
  STORM = 'STORM',
  FLASHBACK = 'FLASHBACK',
  EVOKE = 'EVOKE',
  EXPLOIT = 'EXPLOIT',
  SNEAK = 'SNEAK',
  NINJUTSU = 'NINJUTSU',
  IMPENDING = 'IMPENDING',
  CLEAVE = 'CLEAVE',
  CONSPIRE = 'CONSPIRE',
  CASUALTY = 'CASUALTY',
  MIRACLE = 'MIRACLE',
  HIDEAWAY = 'HIDEAWAY',
  CASCADE = 'CASCADE',
  PLOT = 'PLOT',
  // Creature mechanics
  OFFSPRING = 'OFFSPRING',
  // Damage modification
  WITHER = 'WITHER',
  TOXIC = 'TOXIC',
  // Death replacement
  PERSIST = 'PERSIST',
  // Dies-and-returns-as-enchantment (Duskmourn Glimmer cycle)
  ENDURING = 'ENDURING',
  // Resolution-time city's blessing grant (Ixalan)
  ASCEND = 'ASCEND',
  // Token decay (Innistrad: Midnight Hunt / TDM decayed counter)
  DECAYED = 'DECAYED',
  // Attack-triggered self-buff (Innistrad: Midnight Hunt)
  TRAINING = 'TRAINING',
  // Equipment that makes its own bearer (Final Fantasy)
  JOB_SELECT = 'JOB_SELECT',
  // Ability words
  EERIE = 'EERIE',
  // Instant/sorcery recast next upkeep (Rise of the Eldrazi; granted by Ojer Pakpatiq)
  REBOUND = 'REBOUND',
}

export const KeywordDisplayNames: Record<Keyword, string> = {
  [Keyword.FLYING]: 'Flying',
  [Keyword.MENACE]: 'Menace',
  [Keyword.INTIMIDATE]: 'Intimidate',
  [Keyword.FEAR]: 'Fear',
  [Keyword.SHADOW]: 'Shadow',
  [Keyword.HORSEMANSHIP]: 'Horsemanship',
  [Keyword.SWAMPWALK]: 'Swampwalk',
  [Keyword.FORESTWALK]: 'Forestwalk',
  [Keyword.ISLANDWALK]: 'Islandwalk',
  [Keyword.MOUNTAINWALK]: 'Mountainwalk',
  [Keyword.PLAINSWALK]: 'Plainswalk',
  [Keyword.DESERTWALK]: 'Desertwalk',
  [Keyword.NONBASIC_LANDWALK]: 'Nonbasic landwalk',
  [Keyword.FIRST_STRIKE]: 'First strike',
  [Keyword.DOUBLE_STRIKE]: 'Double strike',
  [Keyword.TRAMPLE]: 'Trample',
  [Keyword.DEATHTOUCH]: 'Deathtouch',
  [Keyword.LIFELINK]: 'Lifelink',
  [Keyword.VIGILANCE]: 'Vigilance',
  [Keyword.REACH]: 'Reach',
  [Keyword.PROVOKE]: 'Provoke',
  [Keyword.FLANKING]: 'Flanking',
  [Keyword.BANDING]: 'Banding',
  [Keyword.AMPLIFY]: 'Amplify',
  [Keyword.DEFENDER]: 'Defender',
  [Keyword.INDESTRUCTIBLE]: 'Indestructible',
  [Keyword.HEXPROOF]: 'Hexproof',
  [Keyword.SHROUD]: 'Shroud',
  [Keyword.WARD]: 'Ward',
  [Keyword.PROTECTION]: 'Protection',
  [Keyword.HASTE]: 'Haste',
  [Keyword.FLASH]: 'Flash',
  [Keyword.PROWESS]: 'Prowess',
  [Keyword.FLURRY]: 'Flurry',
  [Keyword.CHANGELING]: 'Changeling',
  [Keyword.CRAFT]: 'Craft',
  [Keyword.CONVOKE]: 'Convoke',
  [Keyword.DELVE]: 'Delve',
  [Keyword.AFFINITY]: 'Affinity',
  [Keyword.STORM]: 'Storm',
  [Keyword.FLASHBACK]: 'Flashback',
  [Keyword.EVOKE]: 'Evoke',
  [Keyword.EXPLOIT]: 'Exploit',
  [Keyword.SNEAK]: 'Sneak',
  [Keyword.NINJUTSU]: 'Ninjutsu',
  [Keyword.IMPENDING]: 'Impending',
  [Keyword.CLEAVE]: 'Cleave',
  [Keyword.CONSPIRE]: 'Conspire',
  [Keyword.CASUALTY]: 'Casualty',
  [Keyword.MIRACLE]: 'Miracle',
  [Keyword.HIDEAWAY]: 'Hideaway',
  [Keyword.CASCADE]: 'Cascade',
  [Keyword.PLOT]: 'Plot',
  [Keyword.OFFSPRING]: 'Offspring',
  [Keyword.WITHER]: 'Wither',
  [Keyword.TOXIC]: 'Toxic',
  [Keyword.PERSIST]: 'Persist',
  [Keyword.ENDURING]: 'Enduring',
  [Keyword.ASCEND]: 'Ascend',
  [Keyword.DECAYED]: 'Decayed',
  [Keyword.TRAINING]: 'Training',
  [Keyword.JOB_SELECT]: 'Job select',
  [Keyword.EERIE]: 'Eerie',
  [Keyword.REBOUND]: 'Rebound',
}

/**
 * Ability flags — non-keyword static abilities used as engine flags.
 * These are stored in the projected state's keyword set alongside true keywords.
 */
export enum AbilityFlag {
  CANT_BE_BLOCKED = 'CANT_BE_BLOCKED',
  CANT_BE_BLOCKED_BY_MORE_THAN_ONE = 'CANT_BE_BLOCKED_BY_MORE_THAN_ONE',
  DOESNT_UNTAP = 'DOESNT_UNTAP',
  CANT_BECOME_UNTAPPED = 'CANT_BECOME_UNTAPPED',
  MAY_NOT_UNTAP = 'MAY_NOT_UNTAP',
  CANT_RECEIVE_COUNTERS = 'CANT_RECEIVE_COUNTERS',
  ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS = 'ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS',
}

export const AbilityFlagDisplayNames: Record<AbilityFlag, string> = {
  [AbilityFlag.CANT_BE_BLOCKED]: "Can't be blocked",
  [AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE]: "Can't be blocked by more than one creature",
  [AbilityFlag.DOESNT_UNTAP]: "Doesn't untap during your untap step",
  [AbilityFlag.CANT_BECOME_UNTAPPED]: "Can't become untapped",
  [AbilityFlag.MAY_NOT_UNTAP]: 'You may choose not to untap',
  [AbilityFlag.CANT_RECEIVE_COUNTERS]: "Can't have counters put on it",
  [AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS]: 'Assigns combat damage equal to its toughness rather than its power',
}

/**
 * Zone type enum matching backend Zone.kt
 */
export enum ZoneType {
  LIBRARY = 'Library',
  HAND = 'Hand',
  BATTLEFIELD = 'Battlefield',
  GRAVEYARD = 'Graveyard',
  STACK = 'Stack',
  EXILE = 'Exile',
  COMMAND = 'Command',
  // Cards owned "outside the game" (CR 100.4 / 400.11a) — a player's sideboard, reachable in-game
  // only by wish effects. Private to its owner; opponents never see it.
  SIDEBOARD = 'Sideboard',
}

export function isPublicZone(zoneType: ZoneType): boolean {
  return [ZoneType.BATTLEFIELD, ZoneType.GRAVEYARD, ZoneType.STACK, ZoneType.EXILE, ZoneType.COMMAND].includes(zoneType)
}

export function isHiddenZone(zoneType: ZoneType): boolean {
  return [ZoneType.LIBRARY, ZoneType.HAND, ZoneType.SIDEBOARD].includes(zoneType)
}

export function isSharedZone(zoneType: ZoneType): boolean {
  return [ZoneType.BATTLEFIELD, ZoneType.STACK, ZoneType.EXILE].includes(zoneType)
}

/**
 * Counter type enum matching backend CounterType.kt
 */
export enum CounterType {
  PLUS_ONE_PLUS_ONE = 'PLUS_ONE_PLUS_ONE',
  MINUS_ONE_MINUS_ONE = 'MINUS_ONE_MINUS_ONE',
  PLUS_ONE_PLUS_ZERO = 'PLUS_ONE_PLUS_ZERO',
  PLUS_ZERO_PLUS_ONE = 'PLUS_ZERO_PLUS_ONE',
  MINUS_ONE_MINUS_ZERO = 'MINUS_ONE_MINUS_ZERO',
  MINUS_ZERO_MINUS_ONE = 'MINUS_ZERO_MINUS_ONE',
  LOYALTY = 'LOYALTY',
  CHARGE = 'CHARGE',
  GEM = 'GEM',
  POISON = 'POISON',
  GOLD = 'GOLD',
  PLAGUE = 'PLAGUE',
  TRAP = 'TRAP',
  DEPLETION = 'DEPLETION',
  EGG = 'EGG',
  LORE = 'LORE',
  STUN = 'STUN',
  FINALITY = 'FINALITY',
  SUPPLY = 'SUPPLY',
  FLYING = 'FLYING',
  FIRST_STRIKE = 'FIRST_STRIKE',
  DOUBLE_STRIKE = 'DOUBLE_STRIKE',
  VIGILANCE = 'VIGILANCE',
  LIFELINK = 'LIFELINK',
  INDESTRUCTIBLE = 'INDESTRUCTIBLE',
  DEATHTOUCH = 'DEATHTOUCH',
  TRAMPLE = 'TRAMPLE',
  HEXPROOF = 'HEXPROOF',
  REACH = 'REACH',
  STASH = 'STASH',
  BLIGHT = 'BLIGHT',
  COIN = 'COIN',
  FLOOD = 'FLOOD',
  CHORUS = 'CHORUS',
  DREAM = 'DREAM',
  QUEST = 'QUEST',
  GROWTH = 'GROWTH',
  TIME = 'TIME',
  FEATHER = 'FEATHER',
  HOURGLASS = 'HOURGLASS',
  DECAYED = 'DECAYED',
  HOPE = 'HOPE',
  VERSE = 'VERSE',
  INFLUENCE = 'INFLUENCE',
  BURDEN = 'BURDEN',
  LOOT = 'LOOT',
  WIND = 'WIND',
  NEST = 'NEST',
  PAGE = 'PAGE',
  REV = 'REV',
  SOUL = 'SOUL',
  DIVINITY = 'DIVINITY',
  POSSESSION = 'POSSESSION',
  LANDMARK = 'LANDMARK',
  DREAD = 'DREAD',
  INCUBATION = 'INCUBATION',
  FELLOWSHIP = 'FELLOWSHIP',
  BAIT = 'BAIT',
  BORE = 'BORE',
}

export const CounterTypeDisplayNames: Record<CounterType, string> = {
  [CounterType.PLUS_ONE_PLUS_ONE]: '+1/+1',
  [CounterType.MINUS_ONE_MINUS_ONE]: '-1/-1',
  [CounterType.PLUS_ONE_PLUS_ZERO]: '+1/+0',
  [CounterType.PLUS_ZERO_PLUS_ONE]: '+0/+1',
  [CounterType.MINUS_ONE_MINUS_ZERO]: '-1/-0',
  [CounterType.MINUS_ZERO_MINUS_ONE]: '-0/-1',
  [CounterType.LOYALTY]: 'Loyalty',
  [CounterType.CHARGE]: 'Charge',
  [CounterType.GEM]: 'Gem',
  [CounterType.POISON]: 'Poison',
  [CounterType.GOLD]: 'Gold',
  [CounterType.PLAGUE]: 'Plague',
  [CounterType.TRAP]: 'Trap',
  [CounterType.DEPLETION]: 'Depletion',
  [CounterType.EGG]: 'Egg',
  [CounterType.LORE]: 'Lore',
  [CounterType.STUN]: 'Stun',
  [CounterType.FINALITY]: 'Finality',
  [CounterType.SUPPLY]: 'Supply',
  [CounterType.FLYING]: 'Flying',
  [CounterType.FIRST_STRIKE]: 'First Strike',
  [CounterType.DOUBLE_STRIKE]: 'Double Strike',
  [CounterType.VIGILANCE]: 'Vigilance',
  [CounterType.LIFELINK]: 'Lifelink',
  [CounterType.INDESTRUCTIBLE]: 'Indestructible',
  [CounterType.DEATHTOUCH]: 'Deathtouch',
  [CounterType.TRAMPLE]: 'Trample',
  [CounterType.HEXPROOF]: 'Hexproof',
  [CounterType.REACH]: 'Reach',
  [CounterType.STASH]: 'Stash',
  [CounterType.BLIGHT]: 'Blight',
  [CounterType.COIN]: 'Coin',
  [CounterType.FLOOD]: 'Flood',
  [CounterType.CHORUS]: 'Chorus',
  [CounterType.DREAM]: 'Dream',
  [CounterType.QUEST]: 'Quest',
  [CounterType.GROWTH]: 'Growth',
  [CounterType.TIME]: 'Time',
  [CounterType.FEATHER]: 'Feather',
  [CounterType.HOURGLASS]: 'Hourglass',
  [CounterType.DECAYED]: 'Decayed',
  [CounterType.HOPE]: 'Hope',
  [CounterType.VERSE]: 'Verse',
  [CounterType.INFLUENCE]: 'Influence',
  [CounterType.BURDEN]: 'Burden',
  [CounterType.LOOT]: 'Loot',
  [CounterType.WIND]: 'Wind',
  [CounterType.NEST]: 'Nest',
  [CounterType.PAGE]: 'Page',
  [CounterType.REV]: 'Rev',
  [CounterType.SOUL]: 'Soul',
  [CounterType.DIVINITY]: 'Divinity',
  [CounterType.POSSESSION]: 'Possession',
  [CounterType.LANDMARK]: 'Landmark',
  [CounterType.DREAD]: 'Dread',
  [CounterType.INCUBATION]: 'Incubation',
  [CounterType.FELLOWSHIP]: 'Fellowship',
  [CounterType.BAIT]: 'Bait',
  [CounterType.BORE]: 'Bore',
}

/**
 * Error codes from server
 */
export enum ErrorCode {
  NOT_CONNECTED = 'NOT_CONNECTED',
  ALREADY_CONNECTED = 'ALREADY_CONNECTED',
  GAME_NOT_FOUND = 'GAME_NOT_FOUND',
  GAME_FULL = 'GAME_FULL',
  NOT_YOUR_TURN = 'NOT_YOUR_TURN',
  INVALID_ACTION = 'INVALID_ACTION',
  INVALID_DECK = 'INVALID_DECK',
  INTERNAL_ERROR = 'INTERNAL_ERROR',
}

/**
 * Game over reasons
 */
export enum GameOverReason {
  LIFE_ZERO = 'LIFE_ZERO',
  DECK_OUT = 'DECK_OUT',
  CONCESSION = 'CONCESSION',
  POISON_COUNTERS = 'POISON_COUNTERS',
  DISCONNECTION = 'DISCONNECTION',
  CARD_EFFECT = 'CARD_EFFECT',
  DRAW = 'DRAW',
}

/**
 * Combat damage step
 */
export enum CombatDamageStep {
  FIRST_STRIKE = 'FIRST_STRIKE',
  REGULAR = 'REGULAR',
}
