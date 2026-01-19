package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.targeting.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * A CardScript defines the complete behavior of a card.
 * This includes all abilities (triggered, activated, static) and spell effects.
 *
 * CardScripts are registered with the AbilityRegistry and are used by the
 * game engine to determine what happens when a card is played, enters the
 * battlefield, or is activated.
 */
@Serializable
data class CardScript(
    /** The name of the card this script is for */
    val cardName: String,

    /** Keywords are defined in the CardDefinition, but can be referenced here for documentation */
    val keywords: Set<Keyword> = emptySet(),

    /** Triggered abilities (e.g., "When this creature enters the battlefield...") */
    val triggeredAbilities: List<TriggeredAbility> = emptyList(),

    /** Activated abilities (e.g., "{T}: Add {G}") */
    val activatedAbilities: List<ActivatedAbility> = emptyList(),

    /** Static abilities (e.g., "Equipped creature gets +2/+2") */
    val staticAbilities: List<StaticAbility> = emptyList(),

    /** Spell effect for sorceries and instants */
    val spellEffect: SpellEffect? = null,

    /** Targeting requirements for the spell (if it targets) */
    val targetRequirements: List<TargetRequirement> = emptyList(),

    /** Additional costs beyond mana (e.g., sacrifice a creature for Natural Order) */
    val additionalCosts: List<AdditionalCost> = emptyList()
) {
    /**
     * Check if this card has any abilities.
     */
    val hasAbilities: Boolean
        get() = triggeredAbilities.isNotEmpty() ||
                activatedAbilities.isNotEmpty() ||
                staticAbilities.isNotEmpty() ||
                spellEffect != null

    /**
     * Check if this card is a vanilla creature (no abilities).
     */
    val isVanilla: Boolean
        get() = !hasAbilities && keywords.isEmpty()

    /**
     * Check if this card is a French vanilla creature (keywords only).
     */
    val isFrenchVanilla: Boolean
        get() = !hasAbilities && keywords.isNotEmpty()

    companion object {
        /**
         * Create an empty script (for vanilla creatures).
         */
        fun empty(cardName: String): CardScript = CardScript(cardName)

        /**
         * Create a script with just keywords (for French vanilla creatures).
         */
        fun withKeywords(cardName: String, vararg keywords: Keyword): CardScript =
            CardScript(cardName, keywords = keywords.toSet())

        /**
         * Create a script for a basic land.
         */
        fun basicLand(landName: String, manaAbility: ActivatedAbility): CardScript =
            CardScript(
                cardName = landName,
                activatedAbilities = listOf(manaAbility)
            )
    }
}

/**
 * Defines what happens when a spell (sorcery or instant) resolves.
 */
@Serializable
data class SpellEffect(
    /** The effect to execute when the spell resolves */
    val effect: Effect,

    /** Optional condition that must be met for the effect to happen */
    val condition: Condition? = null,

    /** Alternative effect if the condition is not met */
    val elseEffect: Effect? = null
) {
    val description: String
        get() = buildString {
            if (condition != null) {
                append(condition.description.replaceFirstChar { it.uppercase() })
                append(", ")
                append(effect.description.replaceFirstChar { it.lowercase() })
            } else {
                append(effect.description)
            }
            if (elseEffect != null) {
                append(". Otherwise, ")
                append(elseEffect.description.replaceFirstChar { it.lowercase() })
            }
        }
}

/**
 * Builder for creating CardScripts with a fluent API.
 */
class CardScriptBuilder(private val cardName: String) {
    private val keywords = mutableSetOf<Keyword>()
    private val triggeredAbilities = mutableListOf<TriggeredAbility>()
    private val activatedAbilities = mutableListOf<ActivatedAbility>()
    private val staticAbilities = mutableListOf<StaticAbility>()
    private var spellEffect: SpellEffect? = null
    private val targetRequirements = mutableListOf<TargetRequirement>()
    private val additionalCosts = mutableListOf<AdditionalCost>()

    fun keyword(keyword: Keyword) = apply {
        keywords.add(keyword)
    }

    fun keywords(vararg kws: Keyword) = apply {
        keywords.addAll(kws)
    }

    fun triggered(trigger: Trigger, effect: Effect, optional: Boolean = false) = apply {
        triggeredAbilities.add(TriggeredAbility.create(trigger, effect, optional))
    }

    fun triggered(ability: TriggeredAbility) = apply {
        triggeredAbilities.add(ability)
    }

    fun activated(
        cost: AbilityCost,
        effect: Effect,
        timing: TimingRestriction = TimingRestriction.INSTANT,
        isManaAbility: Boolean = false
    ) = apply {
        activatedAbilities.add(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = cost,
                effect = effect,
                timingRestriction = timing,
                isManaAbility = isManaAbility
            )
        )
    }

    fun activated(ability: ActivatedAbility) = apply {
        activatedAbilities.add(ability)
    }

    /**
     * Add a mana ability (tap to add mana).
     * Mana abilities resolve immediately without using the stack.
     */
    fun manaAbility(effect: AddManaEffect) = apply {
        activatedAbilities.add(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = effect,
                timingRestriction = TimingRestriction.INSTANT,
                isManaAbility = true
            )
        )
    }

    /**
     * Add a colorless mana ability.
     */
    fun manaAbility(effect: AddColorlessManaEffect) = apply {
        activatedAbilities.add(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Tap,
                effect = effect,
                timingRestriction = TimingRestriction.INSTANT,
                isManaAbility = true
            )
        )
    }

    fun staticAbility(ability: StaticAbility) = apply {
        staticAbilities.add(ability)
    }

    fun grantKeyword(keyword: Keyword, target: StaticTarget = StaticTarget.AttachedCreature) = apply {
        staticAbilities.add(GrantKeyword(keyword, target))
    }

    fun modifyStats(power: Int, toughness: Int, target: StaticTarget = StaticTarget.AttachedCreature) = apply {
        staticAbilities.add(ModifyStats(power, toughness, target))
    }

    fun spell(effect: Effect) = apply {
        spellEffect = SpellEffect(effect)
    }

    fun spell(effect: Effect, condition: Condition, elseEffect: Effect? = null) = apply {
        spellEffect = SpellEffect(effect, condition, elseEffect)
    }

    fun targets(vararg requirements: TargetRequirement) = apply {
        targetRequirements.addAll(requirements)
    }

    /**
     * Add an additional cost to the spell (beyond mana).
     * Example: additionalCost(AdditionalCost.SacrificePermanent(CardFilter.CreatureCard))
     */
    fun additionalCost(cost: AdditionalCost) = apply {
        additionalCosts.add(cost)
    }

    /**
     * Add multiple additional costs.
     */
    fun additionalCosts(vararg costs: AdditionalCost) = apply {
        additionalCosts.addAll(costs)
    }

    /**
     * Convenience method: Sacrifice a permanent as an additional cost.
     */
    fun sacrificeCost(filter: CardFilter, count: Int = 1) = apply {
        additionalCosts.add(AdditionalCost.SacrificePermanent(filter, count))
    }

    /**
     * Convenience method: Discard cards as an additional cost.
     */
    fun discardCost(count: Int = 1, filter: CardFilter = CardFilter.AnyCard) = apply {
        additionalCosts.add(AdditionalCost.DiscardCards(count, filter))
    }

    /**
     * Convenience method: Pay life as an additional cost.
     */
    fun payLifeCost(amount: Int) = apply {
        additionalCosts.add(AdditionalCost.PayLife(amount))
    }

    fun build(): CardScript = CardScript(
        cardName = cardName,
        keywords = keywords,
        triggeredAbilities = triggeredAbilities,
        activatedAbilities = activatedAbilities,
        staticAbilities = staticAbilities,
        spellEffect = spellEffect,
        targetRequirements = targetRequirements,
        additionalCosts = additionalCosts
    )
}

/**
 * DSL entry point for creating a CardScript.
 */
fun cardScript(cardName: String, init: CardScriptBuilder.() -> Unit = {}): CardScript =
    CardScriptBuilder(cardName).apply(init).build()

/**
 * Registry extension to register a CardScript.
 * This registers all abilities from the script to the AbilityRegistry.
 */
fun AbilityRegistry.register(script: CardScript) {
    if (script.triggeredAbilities.isNotEmpty()) {
        registerTriggeredAbilities(script.cardName, script.triggeredAbilities)
    }
    if (script.activatedAbilities.isNotEmpty()) {
        registerActivatedAbilities(script.cardName, script.activatedAbilities)
    }
    if (script.staticAbilities.isNotEmpty()) {
        registerStaticAbilities(script.cardName, script.staticAbilities)
    }
}

/**
 * Repository of CardScripts for a set of cards.
 * This provides a more structured way to manage card scripts than direct AbilityRegistry.
 */
class CardScriptRepository {
    private val scripts = mutableMapOf<String, CardScript>()

    /**
     * Register a card script.
     */
    fun register(script: CardScript) {
        scripts[script.cardName] = script
    }

    /**
     * Register multiple card scripts.
     */
    fun registerAll(vararg cardScripts: CardScript) {
        cardScripts.forEach { register(it) }
    }

    /**
     * Get the script for a card by name.
     */
    fun getScript(cardName: String): CardScript? = scripts[cardName]

    /**
     * Get the script for a card definition.
     */
    fun getScript(definition: CardDefinition): CardScript? = scripts[definition.name]

    /**
     * Check if a script exists for a card.
     */
    fun hasScript(cardName: String): Boolean = cardName in scripts

    /**
     * Get all registered card names.
     */
    fun registeredCards(): Set<String> = scripts.keys.toSet()

    /**
     * Get the count of registered scripts.
     */
    val size: Int get() = scripts.size

    /**
     * Export all scripts to an AbilityRegistry.
     */
    fun exportTo(registry: AbilityRegistry) {
        scripts.values.forEach { registry.register(it) }
    }

    /**
     * Clear all registered scripts.
     */
    fun clear() {
        scripts.clear()
    }
}
