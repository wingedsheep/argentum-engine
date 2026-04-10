package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * You control enchanted permanent.
 * Used for Auras like Annex that steal control of the enchanted permanent.
 */
@SerialName("ControlEnchantedPermanent")
@Serializable
data object ControlEnchantedPermanent : StaticAbility {
    override val description: String = "You control enchanted permanent"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * A static ability that only applies when a condition is met.
 * Used for cards like Karakyk Guardian: "hexproof if it hasn't dealt damage yet"
 *
 * The engine checks the condition during state projection and only applies
 * the underlying ability's effect when the condition is true.
 *
 * @property ability The underlying static ability to apply when condition is met
 * @property condition The condition that must be true for the ability to apply
 */
@SerialName("ConditionalStaticAbility")
@Serializable
data class ConditionalStaticAbility(
    val ability: StaticAbility,
    val condition: Condition
) : StaticAbility {
    override val description: String = "${ability.description} ${condition.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newAbility = ability.applyTextReplacement(replacer)
        val newCondition = condition.applyTextReplacement(replacer)
        return if (newAbility !== ability || newCondition !== condition) copy(ability = newAbility, condition = newCondition) else this
    }
}

/**
 * Whenever enchanted land is tapped for mana, its controller adds additional mana.
 * Used for auras like Elvish Guidance: "Whenever enchanted land is tapped for mana,
 * its controller adds an additional {G} for each Elf on the battlefield."
 *
 * This is a triggered mana ability that resolves immediately (doesn't use the stack).
 * The engine checks for this ability after a mana ability on the enchanted land resolves.
 *
 * @property color The color of additional mana to produce
 * @property amount How much additional mana to produce (evaluated dynamically)
 */
@SerialName("AdditionalManaOnTap")
@Serializable
data class AdditionalManaOnTap(
    val color: Color,
    val amount: DynamicAmount
) : StaticAbility {
    override val description: String = "Whenever enchanted land is tapped for mana, its controller adds additional mana"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Play with the top card of your library revealed.
 * You may play lands and cast spells from the top of your library.
 * Used for Future Sight.
 */
@SerialName("PlayFromTopOfLibrary")
@Serializable
data object PlayFromTopOfLibrary : StaticAbility {
    override val description: String =
        "Play with the top card of your library revealed. You may play lands and cast spells from the top of your library."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * You may cast spells matching a filter from the top of your library.
 * Unlike PlayFromTopOfLibrary, this only allows specific spell types (not all spells/lands).
 * Used for Precognition Field (instant and sorcery only).
 *
 * @property filter The filter that spells on top of library must match to be castable
 */
@SerialName("CastSpellTypesFromTopOfLibrary")
@Serializable
data class CastSpellTypesFromTopOfLibrary(
    val filter: GameObjectFilter
) : StaticAbility {
    override val description: String =
        "You may cast ${filter.description} spells from the top of your library."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * You may look at the top card of your library any time.
 * Unlike PlayFromTopOfLibrary, this only reveals the top card privately to the controller,
 * not to all players. Used for Lens of Clarity, Vizier of the Menagerie, etc.
 */
@SerialName("LookAtTopOfLibrary")
@Serializable
data object LookAtTopOfLibrary : StaticAbility {
    override val description: String =
        "You may look at the top card of your library any time."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * You may play lands and cast spells matching a filter from the top of your library.
 * Unlike PlayFromTopOfLibrary, this restricts which spells can be cast (but always allows lands).
 * Used for Glarb, Calamity's Augur (mana value 4 or greater).
 *
 * @property spellFilter The filter that spells on top of library must match to be castable
 */
@SerialName("PlayLandsAndCastFilteredFromTopOfLibrary")
@Serializable
data class PlayLandsAndCastFilteredFromTopOfLibrary(
    val spellFilter: GameObjectFilter
) : StaticAbility {
    override val description: String =
        "You may play lands and cast spells matching ${spellFilter.description} from the top of your library."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * You may look at face-down creatures you don't control any time.
 * Reveals the identity of opponent's face-down creatures to the controller.
 * Used for Lens of Clarity.
 */
@SerialName("LookAtFaceDownCreatures")
@Serializable
data object LookAtFaceDownCreatures : StaticAbility {
    override val description: String =
        "You may look at face-down creatures you don't control any time."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * You may cast this card from specified zones (e.g., graveyard, exile).
 * This is an intrinsic property of the card, checked when the card is in a matching zone.
 * Used for Squee, the Immortal (graveyard + exile).
 *
 * @property zones The zones from which this card may be cast
 */
@SerialName("MayCastSelfFromZones")
@Serializable
data class MayCastSelfFromZones(
    val zones: List<Zone>
) : StaticAbility {
    override val description: String = "You may cast this card from ${
        zones.joinToString(" or ") { it.displayName }
    }."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * During each of your turns, you may play a land and cast a permanent spell of each
 * permanent type from your graveyard. Each permanent type (artifact, creature, enchantment,
 * land, planeswalker) can only be used once per turn per source of this ability.
 * If a card has multiple permanent types, you choose one as you play it.
 * Used for Muldrotha, the Gravetide.
 */
@SerialName("MayPlayPermanentsFromGraveyard")
@Serializable
data object MayPlayPermanentsFromGraveyard : StaticAbility {
    override val description: String =
        "During each of your turns, you may play a land and cast a permanent spell of each permanent type from your graveyard."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Prevents all players from cycling cards.
 * Used for Stabilizer: "Players can't cycle cards."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * when determining if cycling/typecycling is a legal action.
 */
@SerialName("PreventCycling")
@Serializable
data object PreventCycling : StaticAbility {
    override val description: String = "Players can't cycle cards"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Prevents mana pools from emptying as steps and phases end.
 * Used for Upwelling: "Players don't lose unspent mana as steps and phases end."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * during mana pool cleanup. While any permanent with this ability is on the battlefield,
 * mana pools are not emptied.
 */
@SerialName("PreventManaPoolEmptying")
@Serializable
data object PreventManaPoolEmptying : StaticAbility {
    override val description: String = "Players don't lose unspent mana as steps and phases end"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Reveal the first card the controller draws each turn.
 * Used for Primitive Etchings and similar "reveal as you draw" effects.
 *
 * The engine checks for this static ability during draws. When the controller
 * draws their first card of a turn and this ability is active, the drawn card
 * is revealed (a CardRevealedFromDrawEvent is emitted). Other triggered abilities
 * can then trigger off that reveal event.
 */
@SerialName("RevealFirstDrawEachTurn")
@Serializable
data object RevealFirstDrawEachTurn : StaticAbility {
    override val description: String = "Reveal the first card you draw each turn"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Replaces land mana production when a land would produce two or more mana.
 * Used for Damping Sphere: "If a land is tapped for two or more mana, it produces {C} instead
 * of any other type and amount."
 *
 * This is a global replacement effect — it applies to all lands for all players.
 * The engine checks for this when resolving mana abilities from lands. If the mana ability
 * would add 2+ total mana, it instead adds only one colorless mana.
 * The ManaSolver also accounts for this when calculating available mana sources.
 */
@SerialName("DampLandManaProduction")
@Serializable
data object DampLandManaProduction : StaticAbility {
    override val description: String = "If a land is tapped for two or more mana, it produces {C} instead of any other type and amount"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Untap all permanents you control during each other player's untap step.
 * Used for Seedborn Muse and similar effects.
 *
 * The engine checks for this static ability during the untap step. When the active
 * player is not the controller of a permanent with this ability, all permanents
 * controlled by the ability's controller are untapped as well.
 */
@SerialName("UntapDuringOtherUntapSteps")
@Serializable
data object UntapDuringOtherUntapSteps : StaticAbility {
    override val description: String = "Untap all permanents you control during each other player's untap step"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Untap permanents matching a filter you control during each other player's untap step.
 * Used for Ivorytusk Fortress (creatures with +1/+1 counters) and similar effects.
 *
 * The engine checks for this static ability during the untap step. When the active
 * player is not the controller of a permanent with this ability, permanents matching
 * the filter controlled by the ability's controller are untapped.
 */
@SerialName("UntapFilteredDuringOtherUntapSteps")
@Serializable
data class UntapFilteredDuringOtherUntapSteps(
    val filter: GameObjectFilter
) : StaticAbility {
    override val description: String = "Untap each ${filter.description} you control during each other player's untap step"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * You may activate loyalty abilities of planeswalkers you control an extra time each turn.
 * Used for Oath of Teferi: "You may activate the loyalty abilities of planeswalkers you control
 * twice each turn rather than only once."
 *
 * The engine checks for this static ability on the controller's battlefield when validating
 * planeswalker loyalty ability activations. Multiple copies do NOT stack — the maximum is
 * always two activations per planeswalker per turn regardless of how many copies are controlled.
 */
@SerialName("ExtraLoyaltyActivation")
@Serializable
data object ExtraLoyaltyActivation : StaticAbility {
    override val description: String = "You may activate loyalty abilities of planeswalkers you control twice each turn rather than only once"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * When a creature matching [creatureFilter] enters the battlefield under your control,
 * triggered abilities of permanents you control that trigger from that entering
 * trigger an additional time.
 *
 * This models Naban, Dean of Iteration and similar "Panharmonicon for a subtype" effects.
 * The [creatureFilter] restricts which entering creatures cause the doubling (e.g., Wizards).
 *
 * Multiple copies are additive: two copies yield three total triggers, etc.
 */
@SerialName("AdditionalETBTriggers")
@Serializable
data class AdditionalETBTriggers(
    val creatureFilter: GameObjectFilter,
    override val description: String = "Triggered abilities trigger an additional time"
) : StaticAbility {
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * If a source you control would deal noncombat damage to an opponent or a permanent
 * an opponent controls, it deals that much damage plus the bonus amount instead.
 *
 * Used for Artist's Talent Level 3 and similar "noncombat damage amplification" effects.
 * This is a static ability on a permanent — the bonus applies as long as the permanent
 * is on the battlefield with this ability active.
 *
 * @property bonusAmount The flat bonus to add to noncombat damage dealt to opponents
 */
@SerialName("NoncombatDamageBonus")
@Serializable
data class NoncombatDamageBonus(
    val bonusAmount: Int
) : StaticAbility {
    override val description: String =
        "If a source you control would deal noncombat damage to an opponent or a permanent an opponent controls, it deals that much damage plus $bonusAmount instead"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
