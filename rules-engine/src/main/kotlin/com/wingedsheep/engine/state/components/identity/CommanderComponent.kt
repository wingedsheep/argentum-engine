package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Marks a card as a designated commander for the Commander format.
 *
 * Attached at game-init time to each player's commander card. Drives:
 * - command-zone setup ([com.wingedsheep.engine.core.GameInitializer])
 * - cast-from-command-zone permission (see `CastSpellEnumerator`, `CastSpellHandler`)
 * - commander tax — `+2` generic mana per [castsFromCommandZone] when cast from the command zone
 *   (CR 903.8); incremented on cast-commit, not on resolution, so countered commanders still owe
 *   the higher tax next time
 * - CR 903.9a state-based action: when the card sits in graveyard / exile / hand / library,
 *   the owner is prompted to put it into the command zone (see
 *   `CommanderZoneChoiceCheck`). The `Format.alwaysDivertToCommand` flag
 *   short-circuits the prompt with a synchronous replacement-time divert.
 * - commander-damage tracking — combat damage dealt by this entity contributes to the
 *   `commanderDamage` map, gated by absence of `TokenComponent` (CR 903.10a — token copies are
 *   not the commander)
 *
 * @property ownerId The player who owns this commander (i.e., whose deck listed it as commander).
 *   Cached here so cast-permission and divert-on-zone-change checks don't have to walk `OwnerComponent`.
 * @property castsFromCommandZone How many times the owner has cast this card from the command
 *   zone over the course of the game. Drives commander tax.
 */
@Serializable
data class CommanderComponent(
    val ownerId: EntityId,
    val castsFromCommandZone: Int = 0,
) : Component
