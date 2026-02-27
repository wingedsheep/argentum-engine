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
  // Combat
  FIRST_STRIKE = 'FIRST_STRIKE',
  DOUBLE_STRIKE = 'DOUBLE_STRIKE',
  TRAMPLE = 'TRAMPLE',
  DEATHTOUCH = 'DEATHTOUCH',
  LIFELINK = 'LIFELINK',
  VIGILANCE = 'VIGILANCE',
  REACH = 'REACH',
  PROVOKE = 'PROVOKE',
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
  CHANGELING = 'CHANGELING',
  // Cost reduction
  CONVOKE = 'CONVOKE',
  DELVE = 'DELVE',
  AFFINITY = 'AFFINITY',
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
  [Keyword.FIRST_STRIKE]: 'First strike',
  [Keyword.DOUBLE_STRIKE]: 'Double strike',
  [Keyword.TRAMPLE]: 'Trample',
  [Keyword.DEATHTOUCH]: 'Deathtouch',
  [Keyword.LIFELINK]: 'Lifelink',
  [Keyword.VIGILANCE]: 'Vigilance',
  [Keyword.REACH]: 'Reach',
  [Keyword.PROVOKE]: 'Provoke',
  [Keyword.DEFENDER]: 'Defender',
  [Keyword.INDESTRUCTIBLE]: 'Indestructible',
  [Keyword.HEXPROOF]: 'Hexproof',
  [Keyword.SHROUD]: 'Shroud',
  [Keyword.WARD]: 'Ward',
  [Keyword.PROTECTION]: 'Protection',
  [Keyword.HASTE]: 'Haste',
  [Keyword.FLASH]: 'Flash',
  [Keyword.PROWESS]: 'Prowess',
  [Keyword.CHANGELING]: 'Changeling',
  [Keyword.CONVOKE]: 'Convoke',
  [Keyword.DELVE]: 'Delve',
  [Keyword.AFFINITY]: 'Affinity',
}

/**
 * Ability flags — non-keyword static abilities used as engine flags.
 * These are stored in the projected state's keyword set alongside true keywords.
 */
export enum AbilityFlag {
  CANT_BE_BLOCKED = 'CANT_BE_BLOCKED',
  CANT_BE_BLOCKED_BY_MORE_THAN_ONE = 'CANT_BE_BLOCKED_BY_MORE_THAN_ONE',
  DOESNT_UNTAP = 'DOESNT_UNTAP',
  MAY_NOT_UNTAP = 'MAY_NOT_UNTAP',
}

export const AbilityFlagDisplayNames: Record<AbilityFlag, string> = {
  [AbilityFlag.CANT_BE_BLOCKED]: "Can't be blocked",
  [AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE]: "Can't be blocked by more than one creature",
  [AbilityFlag.DOESNT_UNTAP]: "Doesn't untap during your untap step",
  [AbilityFlag.MAY_NOT_UNTAP]: 'You may choose not to untap',
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
}

export function isPublicZone(zoneType: ZoneType): boolean {
  return [ZoneType.BATTLEFIELD, ZoneType.GRAVEYARD, ZoneType.STACK, ZoneType.EXILE, ZoneType.COMMAND].includes(zoneType)
}

export function isHiddenZone(zoneType: ZoneType): boolean {
  return [ZoneType.LIBRARY, ZoneType.HAND].includes(zoneType)
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
  LOYALTY = 'LOYALTY',
  CHARGE = 'CHARGE',
  POISON = 'POISON',
  GOLD = 'GOLD',
  PLAGUE = 'PLAGUE',
}

export const CounterTypeDisplayNames: Record<CounterType, string> = {
  [CounterType.PLUS_ONE_PLUS_ONE]: '+1/+1',
  [CounterType.MINUS_ONE_MINUS_ONE]: '-1/-1',
  [CounterType.LOYALTY]: 'Loyalty',
  [CounterType.CHARGE]: 'Charge',
  [CounterType.POISON]: 'Poison',
  [CounterType.GOLD]: 'Gold',
  [CounterType.PLAGUE]: 'Plague',
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
  DRAW = 'DRAW',
}

/**
 * Combat damage step
 */
export enum CombatDamageStep {
  FIRST_STRIKE = 'FIRST_STRIKE',
  REGULAR = 'REGULAR',
}
