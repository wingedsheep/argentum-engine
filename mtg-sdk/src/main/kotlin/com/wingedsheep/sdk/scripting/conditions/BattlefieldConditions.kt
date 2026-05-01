package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Battlefield Conditions (non-generic — require special evaluation logic)
// =============================================================================

/**
 * Condition: "If a player controls more [subtype] creatures than each other player"
 * Used by Thoughtbound Primoc and similar Onslaught "tribal war" cards.
 * Returns true only if exactly one player has strictly more than all others.
 */
@SerialName("APlayerControlsMostOfSubtype")
@Serializable
data class APlayerControlsMostOfSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if a player controls more ${subtype.value}s than each other player"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val new = replacer.replaceSubtype(subtype)
        return if (new == subtype) this else APlayerControlsMostOfSubtype(new)
    }
}

/**
 * Condition: "If you control more creatures of the chosen type than each other player"
 * Used by Peer Pressure-style effects where a creature type is chosen via
 * ChooseOptionEffect and stored in EffectContext.chosenValues[chosenValueKey].
 * Returns true if the controller has strictly more creatures of that type than
 * each other player.
 */
@SerialName("YouControlMostOfChosenType")
@Serializable
data class YouControlMostOfChosenType(val chosenValueKey: String = "chosenCreatureType") : Condition {
    override val description: String = "if you control more creatures of the chosen type than each other player"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If enchanted creature is a [subtype]"
 * Used by auras like Lavamancer's Skill that have different effects based on
 * the creature type of the enchanted creature.
 */
@SerialName("EnchantedCreatureHasSubtype")
@Serializable
data class EnchantedCreatureHasSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if enchanted creature is a ${subtype.value}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val new = replacer.replaceSubtype(subtype)
        return if (new == subtype) this else EnchantedCreatureHasSubtype(new)
    }
}

/**
 * Condition: "If enchanted creature is legendary"
 * Used by auras whose continuous effects apply only while the enchanted creature
 * has the legendary supertype.
 */
@SerialName("EnchantedCreatureIsLegendary")
@Serializable
data object EnchantedCreatureIsLegendary : Condition {
    override val description: String = "if enchanted creature is legendary"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "if it was historic" (legendary, artifact, or Saga).
 * Checks the triggering entity's card definition for the historic quality.
 * Used for Curator's Ward's "if it was historic" intervening-if condition.
 */
@SerialName("TriggeringEntityWasHistoric")
@Serializable
data object TriggeringEntityWasHistoric : Condition {
    override val description: String = "if it was historic"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "if it entered or was cast from a graveyard".
 * True when the triggering entity has either EnteredFromGraveyardComponent (reanimated
 * directly from graveyard → battlefield) or CastFromGraveyardComponent (spell was cast
 * from graveyard). Used by Twilight Diviner.
 */
@SerialName("TriggeringEntityEnteredOrWasCastFromGraveyard")
@Serializable
data object TriggeringEntityEnteredOrWasCastFromGraveyard : Condition {
    override val description: String = "if it entered or was cast from a graveyard"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "if it had a -1/-1 counter on it" (intervening-if for dies/leaves triggers).
 * Reads the last-known -1/-1 counter count captured on the dying entity at the moment
 * it left the battlefield (Rule 603.10 / 603.6c). Used by cards like Retched Wretch
 * whose dies trigger only fires when the creature died with -1/-1 counters on it.
 */
@SerialName("TriggeringEntityHadMinusOneMinusOneCounter")
@Serializable
data object TriggeringEntityHadMinusOneMinusOneCounter : Condition {
    override val description: String = "if it had a -1/-1 counter on it"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "with a single target" — true iff the triggering spell or ability
 * has exactly one target chosen. Reads the triggering entity's TargetsComponent.
 * Used by cards like Spinerock Tyrant whose trigger fires only when you cast an
 * instant or sorcery spell with a single target.
 */
@SerialName("TriggeringSpellHasSingleTarget")
@Serializable
data object TriggeringSpellHasSingleTarget : Condition {
    override val description: String = "with a single target"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "if [target] is [filter]"
 * Checks whether a context target matches a GameObjectFilter.
 * Used for cards like Blessing of Belzenlok: "If it's legendary, it also gains lifelink."
 */
@SerialName("TargetMatchesFilter")
@Serializable
data class TargetMatchesFilter(
    val filter: GameObjectFilter,
    val targetIndex: Int = 0
) : Condition {
    override val description: String = "if target matches $filter"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}
