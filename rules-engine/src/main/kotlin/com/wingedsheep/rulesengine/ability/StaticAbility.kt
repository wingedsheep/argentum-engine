package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Static abilities provide continuous effects that don't use the stack.
 * These include effects from enchantments, equipment, and other permanents.
 */
@Serializable
sealed interface StaticAbility {
    val description: String
}

/**
 * Grants keywords to creatures (e.g., Equipment granting flying).
 */
@Serializable
data class GrantKeyword(
    val keyword: Keyword,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "Grants ${keyword.name.lowercase().replace('_', ' ')}"
}

/**
 * Modifies power/toughness (e.g., +2/+2 from an Equipment).
 */
@Serializable
data class ModifyStats(
    val powerBonus: Int,
    val toughnessBonus: Int,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerBonus >= 0) "+$powerBonus" else "$powerBonus"
        val toughStr = if (toughnessBonus >= 0) "+$toughnessBonus" else "$toughnessBonus"
        append("$powerStr/$toughStr")
    }
}

/**
 * Global effect that affects multiple permanents.
 */
@Serializable
data class GlobalEffect(
    val effectType: GlobalEffectType,
    val filter: CreatureFilter = CreatureFilter.All
) : StaticAbility {
    override val description: String = effectType.description
}

/**
 * Prevents a creature from blocking.
 * Used for cards like Jungle Lion or effects like "Target creature can't block".
 */
@Serializable
data class CantBlock(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "${target.toString().lowercase()} can't block"
}

/**
 * Types of global effects from enchantments.
 */
@Serializable
enum class GlobalEffectType(val description: String) {
    ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE("All creatures get +1/+1"),
    YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE("Creatures you control get +1/+1"),
    OPPONENT_CREATURES_GET_MINUS_ONE_MINUS_ONE("Creatures your opponents control get -1/-1"),
    ALL_CREATURES_HAVE_FLYING("All creatures have flying"),
    YOUR_CREATURES_HAVE_VIGILANCE("Creatures you control have vigilance"),
    YOUR_CREATURES_HAVE_LIFELINK("Creatures you control have lifelink"),
    CREATURES_CANT_ATTACK("Creatures can't attack"),
    CREATURES_CANT_BLOCK("Creatures can't block")
}

/**
 * Filter for which creatures are affected by a static ability.
 */
@Serializable
sealed interface CreatureFilter {
    fun matches(creature: CardInstance, sourceController: PlayerId, state: GameState): Boolean

    @Serializable
    data object All : CreatureFilter {
        override fun matches(creature: CardInstance, sourceController: PlayerId, state: GameState): Boolean = true
    }

    @Serializable
    data object YouControl : CreatureFilter {
        override fun matches(creature: CardInstance, sourceController: PlayerId, state: GameState): Boolean =
            creature.controllerId == sourceController.value
    }

    @Serializable
    data object OpponentsControl : CreatureFilter {
        override fun matches(creature: CardInstance, sourceController: PlayerId, state: GameState): Boolean =
            creature.controllerId != sourceController.value
    }

    @Serializable
    data class WithKeyword(val keyword: Keyword) : CreatureFilter {
        override fun matches(creature: CardInstance, sourceController: PlayerId, state: GameState): Boolean =
            creature.hasKeyword(keyword)
    }

    @Serializable
    data class WithoutKeyword(val keyword: Keyword) : CreatureFilter {
        override fun matches(creature: CardInstance, sourceController: PlayerId, state: GameState): Boolean =
            !creature.hasKeyword(keyword)
    }
}

/**
 * Target for static abilities (what the ability affects).
 */
@Serializable
sealed interface StaticTarget {
    @Serializable
    data object AttachedCreature : StaticTarget

    @Serializable
    data object SourceCreature : StaticTarget

    @Serializable
    data object Controller : StaticTarget

    @Serializable
    data class SpecificCard(val cardId: CardId) : StaticTarget
}

/**
 * Applies static abilities to calculate the current game state.
 * Static abilities are applied in layers according to MTG rules.
 */
object StaticAbilityApplier {

    /**
     * Get all stat bonuses for a creature from attached equipment and auras.
     * Returns a pair of (powerBonus, toughnessBonus).
     */
    fun getStatBonuses(creature: CardInstance, state: GameState, registry: AbilityRegistry): Pair<Int, Int> {
        var powerBonus = 0
        var toughnessBonus = 0

        // Find all attachments on this creature
        val attachments = state.battlefield.cards.filter { it.attachedTo == creature.id }

        for (attachment in attachments) {
            val abilities = registry.getStaticAbilities(attachment.definition)
            for (ability in abilities) {
                when (ability) {
                    is ModifyStats -> {
                        if (ability.target is StaticTarget.AttachedCreature) {
                            powerBonus += ability.powerBonus
                            toughnessBonus += ability.toughnessBonus
                        }
                    }
                    else -> { /* Handled elsewhere */ }
                }
            }
        }

        // Apply global effects from enchantments
        for (permanent in state.battlefield.cards) {
            if (permanent.isEnchantment && !permanent.isAura) {
                val abilities = registry.getStaticAbilities(permanent.definition)
                for (ability in abilities) {
                    when (ability) {
                        is GlobalEffect -> {
                            val controllerId = PlayerId.of(permanent.controllerId)
                            if (ability.filter.matches(creature, controllerId, state)) {
                                when (ability.effectType) {
                                    GlobalEffectType.ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE -> {
                                        powerBonus += 1
                                        toughnessBonus += 1
                                    }
                                    GlobalEffectType.YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE -> {
                                        if (creature.controllerId == permanent.controllerId) {
                                            powerBonus += 1
                                            toughnessBonus += 1
                                        }
                                    }
                                    GlobalEffectType.OPPONENT_CREATURES_GET_MINUS_ONE_MINUS_ONE -> {
                                        if (creature.controllerId != permanent.controllerId) {
                                            powerBonus -= 1
                                            toughnessBonus -= 1
                                        }
                                    }
                                    else -> { /* Not a stat modifier */ }
                                }
                            }
                        }
                        else -> { /* Not a global effect */ }
                    }
                }
            }
        }

        return powerBonus to toughnessBonus
    }

    /**
     * Get all keywords granted to a creature from attached equipment and auras.
     */
    fun getGrantedKeywords(creature: CardInstance, state: GameState, registry: AbilityRegistry): Set<Keyword> {
        val grantedKeywords = mutableSetOf<Keyword>()

        // Find all attachments on this creature
        val attachments = state.battlefield.cards.filter { it.attachedTo == creature.id }

        for (attachment in attachments) {
            val abilities = registry.getStaticAbilities(attachment.definition)
            for (ability in abilities) {
                when (ability) {
                    is GrantKeyword -> {
                        if (ability.target is StaticTarget.AttachedCreature) {
                            grantedKeywords.add(ability.keyword)
                        }
                    }
                    else -> { /* Handled elsewhere */ }
                }
            }
        }

        // Apply global effects from enchantments
        for (permanent in state.battlefield.cards) {
            if (permanent.isEnchantment && !permanent.isAura) {
                val abilities = registry.getStaticAbilities(permanent.definition)
                for (ability in abilities) {
                    when (ability) {
                        is GlobalEffect -> {
                            val controllerId = PlayerId.of(permanent.controllerId)
                            if (ability.filter.matches(creature, controllerId, state)) {
                                when (ability.effectType) {
                                    GlobalEffectType.ALL_CREATURES_HAVE_FLYING -> {
                                        grantedKeywords.add(Keyword.FLYING)
                                    }
                                    GlobalEffectType.YOUR_CREATURES_HAVE_VIGILANCE -> {
                                        if (creature.controllerId == permanent.controllerId) {
                                            grantedKeywords.add(Keyword.VIGILANCE)
                                        }
                                    }
                                    GlobalEffectType.YOUR_CREATURES_HAVE_LIFELINK -> {
                                        if (creature.controllerId == permanent.controllerId) {
                                            grantedKeywords.add(Keyword.LIFELINK)
                                        }
                                    }
                                    else -> { /* Not a keyword granter */ }
                                }
                            }
                        }
                        else -> { /* Not a global effect */ }
                    }
                }
            }
        }

        return grantedKeywords
    }
}
