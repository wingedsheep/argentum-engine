import { ZoneType } from './enums'

/**
 * Branded type for entity IDs.
 * EntityIds are strings that uniquely identify game entities (cards, players, abilities, etc.)
 */
export type EntityId = string & { readonly __brand: 'EntityId' }

/**
 * Create an EntityId from a string.
 * Use this when receiving IDs from the server.
 */
export function entityId(value: string): EntityId {
  return value as EntityId
}

/**
 * Zone identifier combining zone type with optional owner.
 * Shared zones (battlefield, stack, exile) have no owner.
 * Player zones (library, hand, graveyard) have an owner.
 */
export interface ZoneId {
  readonly type: ZoneType
  readonly ownerId?: EntityId
}

/**
 * Create a ZoneId for a shared zone.
 */
export function sharedZone(type: ZoneType.BATTLEFIELD | ZoneType.STACK | ZoneType.EXILE | ZoneType.COMMAND): ZoneId {
  return { type }
}

/**
 * Create a ZoneId for a player-owned zone.
 */
export function playerZone(type: ZoneType.LIBRARY | ZoneType.HAND | ZoneType.GRAVEYARD, ownerId: EntityId): ZoneId {
  return { type, ownerId }
}

/**
 * Predefined shared zone IDs.
 */
export const BATTLEFIELD: ZoneId = { type: ZoneType.BATTLEFIELD }
export const STACK: ZoneId = { type: ZoneType.STACK }
export const EXILE: ZoneId = { type: ZoneType.EXILE }
export const COMMAND: ZoneId = { type: ZoneType.COMMAND }

/**
 * Create a library zone for a player.
 */
export function library(playerId: EntityId): ZoneId {
  return { type: ZoneType.LIBRARY, ownerId: playerId }
}

/**
 * Create a hand zone for a player.
 */
export function hand(playerId: EntityId): ZoneId {
  return { type: ZoneType.HAND, ownerId: playerId }
}

/**
 * Create a graveyard zone for a player.
 */
export function graveyard(playerId: EntityId): ZoneId {
  return { type: ZoneType.GRAVEYARD, ownerId: playerId }
}

/**
 * Check if two zone IDs are equal.
 */
export function zoneIdEquals(a: ZoneId, b: ZoneId): boolean {
  return a.type === b.type && a.ownerId === b.ownerId
}

/**
 * Convert a zone ID to a string for use as map keys, etc.
 */
export function zoneIdToString(zoneId: ZoneId): string {
  return zoneId.ownerId ? `${zoneId.type}:${zoneId.ownerId}` : zoneId.type
}

/**
 * Parse a zone ID from its string representation.
 */
export function parseZoneId(str: string): ZoneId {
  const parts = str.split(':')
  const type = parts[0] as ZoneType
  if (parts[1]) {
    return { type, ownerId: entityId(parts[1]) }
  }
  return { type }
}
