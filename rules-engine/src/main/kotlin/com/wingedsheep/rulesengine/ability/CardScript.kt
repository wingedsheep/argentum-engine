package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.targeting.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * A CardScript defines the complete behavior of a card.
 * This includes all abilities (triggered, activated, static) and spell effects.
 */
@Serializable
data class CardScript(
    val cardName: String,
    val keywords: Set<Keyword> = emptySet(),
    val triggeredAbilities: List<TriggeredAbility> = emptyList(),
    val activatedAbilities: List<ActivatedAbility> = emptyList(),
    val staticAbilities: List<StaticAbility> = emptyList(),
    val spellEffect: SpellEffect? = null,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val additionalCosts: List<AdditionalCost> = emptyList()
) {
    val hasAbilities: Boolean
        get() = triggeredAbilities.isNotEmpty() ||
                activatedAbilities.isNotEmpty() ||
                staticAbilities.isNotEmpty() ||
                spellEffect != null

    val isVanilla: Boolean
        get() = !hasAbilities && keywords.isEmpty()

    val isFrenchVanilla: Boolean
        get() = !hasAbilities && keywords.isNotEmpty()

    companion object {
        fun empty(cardName: String): CardScript = CardScript(cardName)

        fun withKeywords(cardName: String, vararg keywords: Keyword): CardScript =
            CardScript(cardName, keywords = keywords.toSet())

        fun basicLand(landName: String, manaAbility: ActivatedAbility): CardScript =
            CardScript(
                cardName = landName,
                activatedAbilities = listOf(manaAbility)
            )
    }
}

@Serializable
data class SpellEffect(
    val effect: Effect,
    val condition: Condition? = null,
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

@JvmInline
value class TargetReference(val index: Int)

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

    // --- Keyword Macros ---

    /**
     * Adds Prowess: "Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn."
     */
    fun prowess() = apply {
        keyword(Keyword.PROWESS)
        triggered(
            trigger = OnSpellCast(
                controllerOnly = true,
                spellType = SpellTypeFilter.NONCREATURE
            ),
            effect = ModifyStatsEffect(
                powerModifier = 1,
                toughnessModifier = 1,
                target = EffectTarget.Self,
                untilEndOfTurn = true
            )
        )
    }

    // --- Ability Builders ---

    fun triggered(
        trigger: Trigger,
        effect: Effect,
        optional: Boolean = false,
        targetRequirement: TargetRequirement? = null
    ) = apply {
        triggeredAbilities.add(TriggeredAbility.create(trigger, effect, optional, targetRequirement))
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

    fun manaAbility(effect: AddAnyColorManaEffect) = apply {
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
     * Add a planeswalker loyalty ability.
     * @param loyaltyCost Positive for +X abilities, negative for -X abilities
     * @param effect The effect of the ability
     */
    fun planeswalkerAbility(loyaltyCost: Int, effect: Effect) = apply {
        activatedAbilities.add(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Loyalty(loyaltyCost),
                effect = effect,
                timingRestriction = TimingRestriction.SORCERY,  // Planeswalker abilities are sorcery speed
                isManaAbility = false,
                isPlaneswalkerAbility = true
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

    fun cantReceiveCounters(target: StaticTarget = StaticTarget.AttachedCreature) = apply {
        staticAbilities.add(CantReceiveCounters(target))
    }

    fun spell(effect: Effect) = apply {
        spellEffect = SpellEffect(effect)
    }

    fun spell(effect: Effect, condition: Condition, elseEffect: Effect? = null) = apply {
        spellEffect = SpellEffect(effect, condition, elseEffect)
    }

    fun targets(requirement: TargetRequirement): TargetReference {
        targetRequirements.add(requirement)
        return TargetReference(targetRequirements.lastIndex)
    }

    fun targets(vararg requirements: TargetRequirement) = apply {
        targetRequirements.addAll(requirements)
    }

    fun additionalCost(cost: AdditionalCost) = apply {
        additionalCosts.add(cost)
    }

    fun additionalCosts(vararg costs: AdditionalCost) = apply {
        additionalCosts.addAll(costs)
    }

    fun sacrificeCost(filter: CardFilter, count: Int = 1) = apply {
        additionalCosts.add(AdditionalCost.SacrificePermanent(filter, count))
    }

    fun discardCost(count: Int = 1, filter: CardFilter = CardFilter.AnyCard) = apply {
        additionalCosts.add(AdditionalCost.DiscardCards(count, filter))
    }

    fun payLifeCost(amount: Int) = apply {
        additionalCosts.add(AdditionalCost.PayLife(amount))
    }

    /**
     * Cycling {cost}: Discard this card: Draw a card.
     * An activated ability from hand.
     */
    fun cycling(manaCost: AbilityCost.Mana) = apply {
        activatedAbilities.add(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Composite(listOf(manaCost, AbilityCost.DiscardSelf)),
                effect = DrawCardsEffect(count = 1, target = EffectTarget.Controller),
                timingRestriction = TimingRestriction.INSTANT,
                activateFromZone = com.wingedsheep.rulesengine.zone.ZoneType.HAND
            )
        )
    }

    /**
     * Basic landcycling {cost}: Discard this card: Search for a basic land card.
     * An activated ability from hand.
     */
    fun basicLandcycling(manaCost: AbilityCost.Mana) = apply {
        activatedAbilities.add(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Composite(listOf(manaCost, AbilityCost.DiscardSelf)),
                effect = SearchLibraryEffect(
                    filter = CardFilter.BasicLandCard,
                    count = 1,
                    destination = SearchDestination.HAND,
                    reveal = true,
                    shuffleAfter = true
                ),
                timingRestriction = TimingRestriction.SORCERY, // Cycling is sorcery speed
                activateFromZone = com.wingedsheep.rulesengine.zone.ZoneType.HAND
            )
        )
    }

    /**
     * Type cycling for specific land types (e.g., Forestcycling, Islandcycling).
     */
    fun landTypeCycling(landType: String, manaCost: AbilityCost.Mana) = apply {
        activatedAbilities.add(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Composite(listOf(manaCost, AbilityCost.DiscardSelf)),
                effect = SearchLibraryEffect(
                    filter = CardFilter.HasSubtype(landType),
                    count = 1,
                    destination = SearchDestination.HAND,
                    reveal = true,
                    shuffleAfter = true
                ),
                timingRestriction = TimingRestriction.SORCERY,
                activateFromZone = com.wingedsheep.rulesengine.zone.ZoneType.HAND
            )
        )
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

fun cardScript(cardName: String, init: CardScriptBuilder.() -> Unit = {}): CardScript =
    CardScriptBuilder(cardName).apply(init).build()

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

class CardScriptRepository {
    private val scripts = mutableMapOf<String, CardScript>()

    fun register(script: CardScript) {
        scripts[script.cardName] = script
    }

    fun registerAll(vararg cardScripts: CardScript) {
        cardScripts.forEach { register(it) }
    }

    fun getScript(cardName: String): CardScript? = scripts[cardName]
    fun getScript(definition: CardDefinition): CardScript? = scripts[definition.name]
    fun hasScript(cardName: String): Boolean = cardName in scripts
    fun registeredCards(): Set<String> = scripts.keys.toSet()
    val size: Int get() = scripts.size
    fun exportTo(registry: AbilityRegistry) {
        scripts.values.forEach { registry.register(it) }
    }
    fun clear() {
        scripts.clear()
    }
}
