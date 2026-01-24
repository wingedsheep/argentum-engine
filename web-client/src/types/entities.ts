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
 * Matches backend ZoneKey.kt
 * Shared zones (battlefield, stack, exile) have no owner.
 * Player zones (library, hand, graveyard) have an owner.
 */
export interface ZoneId {
  readonly zoneType: ZoneType
  readonly ownerId: EntityId
}

/**
 * Create a ZoneId for a shared zone.
 * Note: Server still sends ownerId even for shared zones (usually player-1 or similar).
 */
export function sharedZone(zoneType: ZoneType.BATTLEFIELD | ZoneType.STACK | ZoneType.EXILE | ZoneType.COMMAND, ownerId: EntityId): ZoneId {
  return { zoneType, ownerId }
}

/**
 * Create a ZoneId for a player-owned zone.
 */
export function playerZone(zoneType: ZoneType.LIBRARY | ZoneType.HAND | ZoneType.GRAVEYARD, ownerId: EntityId): ZoneId {
  return { zoneType, ownerId }
}

/**
 * Create a battlefield zone for a player.
 */
export function battlefield(playerId: EntityId): ZoneId {
  return { zoneType: ZoneType.BATTLEFIELD, ownerId: playerId }
}

/**
 * Create a stack zone for a player.
 */
export function stack(playerId: EntityId): ZoneId {
  return { zoneType: ZoneType.STACK, ownerId: playerId }
}

/**
 * Create an exile zone for a player.
 */
export function exile(playerId: EntityId): ZoneId {
  return { zoneType: ZoneType.EXILE, ownerId: playerId }
}

/**
 * Create a command zone for a player.
 */
export function command(playerId: EntityId): ZoneId {
  return { zoneType: ZoneType.COMMAND, ownerId: playerId }
}

/**
 * Create a library zone for a player.
 */
export function library(playerId: EntityId): ZoneId {
  return { zoneType: ZoneType.LIBRARY, ownerId: playerId }
}

/**
 * Create a hand zone for a player.
 */
export function hand(playerId: EntityId): ZoneId {
  return { zoneType: ZoneType.HAND, ownerId: playerId }
}

/**
 * Create a graveyard zone for a player.
 */
export function graveyard(playerId: EntityId): ZoneId {
  return { zoneType: ZoneType.GRAVEYARD, ownerId: playerId }
}

/**
 * Check if two zone IDs are equal.
 */
export function zoneIdEquals(a: ZoneId, b: ZoneId): boolean {
  return a.zoneType === b.zoneType && a.ownerId === b.ownerId
}

/**
 * Convert a zone ID to a string for use as map keys, etc.
 */
export function zoneIdToString(zoneId: ZoneId): string {
  return `${zoneId.ownerId}:${zoneId.zoneType}`
}

/**
 * Parse a zone ID from its string representation.
 */
export function parseZoneId(str: string): ZoneId {
  const parts = str.split(':')
  const ownerId = entityId(parts[0]!)
  const zoneType = parts[1] as ZoneType
  return { ownerId, zoneType }
}
