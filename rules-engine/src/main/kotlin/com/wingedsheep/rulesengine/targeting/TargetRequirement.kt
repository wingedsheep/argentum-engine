package com.wingedsheep.rulesengine.targeting

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Defines what can be targeted by a spell or ability.
 * Each TargetRequirement specifies the valid targets and any restrictions.
 */
@Serializable
sealed interface TargetRequirement {
    val description: String
    val count: Int get() = 1  // How many targets are required
    val optional: Boolean get() = false  // "up to X" targets

    /**
     * Check if a given target is valid for this requirement.
     */
    fun isValidTarget(
        target: Target,
        state: GameState,
        sourceControllerId: PlayerId,
        sourceId: CardId?
    ): Boolean
}

// =============================================================================
// Player Targeting
// =============================================================================

/**
 * Target player (any player).
 */
@Serializable
data class TargetPlayer(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target player" else "target $count players"

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        return target is Target.PlayerTarget && state.players.containsKey(target.playerId)
    }
}

/**
 * Target opponent only.
 */
@Serializable
data class TargetOpponent(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target opponent" else "target $count opponents"

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        return target is Target.PlayerTarget &&
                target.playerId != sourceControllerId &&
                state.players.containsKey(target.playerId)
    }
}

// =============================================================================
// Creature Targeting
// =============================================================================

/**
 * Target creature (any creature on the battlefield).
 */
@Serializable
data class TargetCreature(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: CreatureTargetFilter = CreatureTargetFilter.Any
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != CreatureTargetFilter.Any) {
            append(filter.description)
            append(" ")
        }
        append(if (count == 1) "creature" else "$count creatures")
    }

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        if (target !is Target.CardTarget) return false

        val creature = state.battlefield.getCard(target.cardId) ?: return false
        if (!creature.isCreature) return false

        return filter.matches(creature, sourceControllerId, state)
    }
}

/**
 * Filter for creature targeting restrictions.
 */
@Serializable
sealed interface CreatureTargetFilter {
    val description: String
    fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean

    @Serializable
    data object Any : CreatureTargetFilter {
        override val description: String = ""
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean = true
    }

    @Serializable
    data object YouControl : CreatureTargetFilter {
        override val description: String = "creature you control"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            creature.controllerId == sourceControllerId.value
    }

    @Serializable
    data object OpponentControls : CreatureTargetFilter {
        override val description: String = "creature an opponent controls"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            creature.controllerId != sourceControllerId.value
    }

    @Serializable
    data object Attacking : CreatureTargetFilter {
        override val description: String = "attacking"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean {
            val combat = state.combat ?: return false
            return combat.attackers.containsKey(creature.id)
        }
    }

    @Serializable
    data object Blocking : CreatureTargetFilter {
        override val description: String = "blocking"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean {
            val combat = state.combat ?: return false
            return combat.blockers.containsKey(creature.id)
        }
    }

    @Serializable
    data object Tapped : CreatureTargetFilter {
        override val description: String = "tapped"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            creature.isTapped
    }

    @Serializable
    data object Untapped : CreatureTargetFilter {
        override val description: String = "untapped"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            !creature.isTapped
    }

    @Serializable
    data class WithKeyword(val keyword: Keyword) : CreatureTargetFilter {
        override val description: String = keyword.name.lowercase().replace('_', ' ')
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            creature.hasKeyword(keyword)
    }

    @Serializable
    data class WithoutKeyword(val keyword: Keyword) : CreatureTargetFilter {
        override val description: String = "without ${keyword.name.lowercase().replace('_', ' ')}"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            !creature.hasKeyword(keyword)
    }

    @Serializable
    data class WithColor(val color: Color) : CreatureTargetFilter {
        override val description: String = color.displayName.lowercase()
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            color in creature.definition.colors
    }

    @Serializable
    data class WithPowerAtMost(val maxPower: Int) : CreatureTargetFilter {
        override val description: String = "with power $maxPower or less"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            (creature.currentPower ?: 0) <= maxPower
    }

    @Serializable
    data class WithPowerAtLeast(val minPower: Int) : CreatureTargetFilter {
        override val description: String = "with power $minPower or greater"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            (creature.currentPower ?: 0) >= minPower
    }

    @Serializable
    data class WithToughnessAtMost(val maxToughness: Int) : CreatureTargetFilter {
        override val description: String = "with toughness $maxToughness or less"
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            (creature.currentToughness ?: 0) <= maxToughness
    }

    /**
     * Combine multiple filters (all must match).
     */
    @Serializable
    data class And(val filters: List<CreatureTargetFilter>) : CreatureTargetFilter {
        override val description: String = filters.joinToString(" ") { it.description }
        override fun matches(creature: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            filters.all { it.matches(creature, sourceControllerId, state) }
    }
}

// =============================================================================
// Permanent Targeting
// =============================================================================

/**
 * Target permanent (any permanent on the battlefield).
 */
@Serializable
data class TargetPermanent(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: PermanentTargetFilter = PermanentTargetFilter.Any
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != PermanentTargetFilter.Any) {
            append(filter.description)
            append(" ")
        }
        append(if (count == 1) "permanent" else "$count permanents")
    }

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        if (target !is Target.CardTarget) return false

        val permanent = state.battlefield.getCard(target.cardId) ?: return false
        if (!permanent.isPermanent) return false

        return filter.matches(permanent, sourceControllerId, state)
    }
}

/**
 * Filter for permanent targeting restrictions.
 */
@Serializable
sealed interface PermanentTargetFilter {
    val description: String
    fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean

    @Serializable
    data object Any : PermanentTargetFilter {
        override val description: String = ""
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean = true
    }

    @Serializable
    data object YouControl : PermanentTargetFilter {
        override val description: String = "permanent you control"
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            permanent.controllerId == sourceControllerId.value
    }

    @Serializable
    data object OpponentControls : PermanentTargetFilter {
        override val description: String = "permanent an opponent controls"
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            permanent.controllerId != sourceControllerId.value
    }

    @Serializable
    data object Creature : PermanentTargetFilter {
        override val description: String = "creature"
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            permanent.isCreature
    }

    @Serializable
    data object Artifact : PermanentTargetFilter {
        override val description: String = "artifact"
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            permanent.isArtifact
    }

    @Serializable
    data object Enchantment : PermanentTargetFilter {
        override val description: String = "enchantment"
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            permanent.isEnchantment
    }

    @Serializable
    data object Land : PermanentTargetFilter {
        override val description: String = "land"
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            permanent.isLand
    }

    @Serializable
    data object NonCreature : PermanentTargetFilter {
        override val description: String = "noncreature"
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            !permanent.isCreature
    }

    @Serializable
    data object NonLand : PermanentTargetFilter {
        override val description: String = "nonland"
        override fun matches(permanent: CardInstance, sourceControllerId: PlayerId, state: GameState): Boolean =
            !permanent.isLand
    }
}

// =============================================================================
// Combined Targeting
// =============================================================================

/**
 * "Any target" - can target any creature, player, or planeswalker.
 */
@Serializable
data class AnyTarget(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "any target" else "$count targets"

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        return when (target) {
            is Target.PlayerTarget -> state.players.containsKey(target.playerId)
            is Target.CardTarget -> {
                val card = state.battlefield.getCard(target.cardId)
                card != null && (card.isCreature || card.definition.typeLine.cardTypes.any {
                    it == com.wingedsheep.rulesengine.core.CardType.PLANESWALKER
                })
            }
        }
    }
}

/**
 * "Target creature or player" - classic burn spell targeting.
 */
@Serializable
data class TargetCreatureOrPlayer(
    override val count: Int = 1,
    override val optional: Boolean = false
) : TargetRequirement {
    override val description: String = if (count == 1) "target creature or player" else "$count targets (creatures or players)"

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        return when (target) {
            is Target.PlayerTarget -> state.players.containsKey(target.playerId)
            is Target.CardTarget -> {
                val card = state.battlefield.getCard(target.cardId)
                card != null && card.isCreature
            }
        }
    }
}

// =============================================================================
// Card Targeting (other zones)
// =============================================================================

/**
 * Target card in a graveyard.
 */
@Serializable
data class TargetCardInGraveyard(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: GraveyardCardFilter = GraveyardCardFilter.Any
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != GraveyardCardFilter.Any) {
            append(filter.description)
            append(" ")
        }
        append("card in a graveyard")
    }

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        if (target !is Target.CardTarget) return false

        // Search all graveyards
        for ((_, player) in state.players) {
            val card = player.graveyard.getCard(target.cardId)
            if (card != null && filter.matches(card)) {
                return true
            }
        }
        return false
    }
}

/**
 * Filter for graveyard card targeting.
 */
@Serializable
sealed interface GraveyardCardFilter {
    val description: String
    fun matches(card: CardInstance): Boolean

    @Serializable
    data object Any : GraveyardCardFilter {
        override val description: String = ""
        override fun matches(card: CardInstance): Boolean = true
    }

    @Serializable
    data object Creature : GraveyardCardFilter {
        override val description: String = "creature"
        override fun matches(card: CardInstance): Boolean = card.isCreature
    }

    @Serializable
    data object Instant : GraveyardCardFilter {
        override val description: String = "instant"
        override fun matches(card: CardInstance): Boolean = card.definition.isInstant
    }

    @Serializable
    data object Sorcery : GraveyardCardFilter {
        override val description: String = "sorcery"
        override fun matches(card: CardInstance): Boolean = card.definition.isSorcery
    }

    @Serializable
    data object InstantOrSorcery : GraveyardCardFilter {
        override val description: String = "instant or sorcery"
        override fun matches(card: CardInstance): Boolean =
            card.definition.isInstant || card.definition.isSorcery
    }
}

// =============================================================================
// Spell Targeting (on stack)
// =============================================================================

/**
 * Target spell on the stack.
 */
@Serializable
data class TargetSpell(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: SpellTargetFilter = SpellTargetFilter.Any
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != SpellTargetFilter.Any) {
            append(filter.description)
            append(" ")
        }
        append("spell")
    }

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        if (target !is Target.CardTarget) return false

        val spell = state.stack.getCard(target.cardId) ?: return false
        return filter.matches(spell)
    }
}

/**
 * Filter for spell targeting.
 */
@Serializable
sealed interface SpellTargetFilter {
    val description: String
    fun matches(spell: CardInstance): Boolean

    @Serializable
    data object Any : SpellTargetFilter {
        override val description: String = ""
        override fun matches(spell: CardInstance): Boolean = true
    }

    @Serializable
    data object Creature : SpellTargetFilter {
        override val description: String = "creature"
        override fun matches(spell: CardInstance): Boolean = spell.isCreature
    }

    @Serializable
    data object Noncreature : SpellTargetFilter {
        override val description: String = "noncreature"
        override fun matches(spell: CardInstance): Boolean = !spell.isCreature
    }

    @Serializable
    data object Instant : SpellTargetFilter {
        override val description: String = "instant"
        override fun matches(spell: CardInstance): Boolean = spell.definition.isInstant
    }

    @Serializable
    data object Sorcery : SpellTargetFilter {
        override val description: String = "sorcery"
        override fun matches(spell: CardInstance): Boolean = spell.definition.isSorcery
    }
}

// =============================================================================
// Special Targeting
// =============================================================================

/**
 * Target another target (for modal spells, redirection, etc.).
 * E.g., "Change the target of target spell with a single target"
 */
@Serializable
data class TargetOther(
    val baseRequirement: TargetRequirement,
    val excludeSource: Boolean = true
) : TargetRequirement {
    override val description: String = "target other ${baseRequirement.description}"
    override val count: Int = baseRequirement.count
    override val optional: Boolean = baseRequirement.optional

    override fun isValidTarget(target: Target, state: GameState, sourceControllerId: PlayerId, sourceId: CardId?): Boolean {
        // Exclude the source if required
        if (excludeSource && sourceId != null && target is Target.CardTarget && target.cardId == sourceId) {
            return false
        }
        return baseRequirement.isValidTarget(target, state, sourceControllerId, sourceId)
    }
}
