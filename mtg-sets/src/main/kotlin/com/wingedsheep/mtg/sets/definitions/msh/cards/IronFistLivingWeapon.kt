package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Iron Fist, Living Weapon — Marvel Super Heroes #138 (uncommon)
 * {2}{R} · Legendary Creature — Human Warrior Hero · 2/3
 *
 * Whenever you cast a spell that targets a creature you control, Iron Fist gains
 * "{T}: Iron Fist deals damage equal to his power to any other target" until end of turn.
 *
 * Two existing pieces, composed:
 *  - the trigger is [Triggers.youCastSpellTargeting] over `Creature.youControl()` — the same
 *    facade Mockingbird, Ace Agent and Colleen Wing use. It fires once per qualifying spell, and
 *    Iron Fist himself is "a creature you control", so a pump aimed at him arms the ability too.
 *  - the payoff is [GrantActivatedAbilityEffect] on [EffectTarget.Self] with the default
 *    `Duration.EndOfTurn` (Run Wild's grant shape). Each qualifying cast grants a *separate*
 *    copy of the ability, but every copy shares the one {T} cost, so the fist still only punches
 *    once per untap — exactly the printed behaviour.
 *
 * Inside the granted ability, "any other target" is [TargetOther] wrapping [AnyTarget]: with no
 * explicit exclusion set, the engine excludes the ability's own source, i.e. Iron Fist. "Damage
 * equal to his power" is [DynamicAmounts.sourcePower], read as the granted ability resolves, so a
 * pump spell that arms the ability also grows the damage.
 */
val IronFistLivingWeapon = card("Iron Fist, Living Weapon") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Warrior Hero"
    oracleText = "Whenever you cast a spell that targets a creature you control, Iron Fist gains " +
        "\"{T}: Iron Fist deals damage equal to his power to any other target\" until end of turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.youCastSpellTargeting(GameObjectFilter.Creature.youControl())
        effect = GrantActivatedAbilityEffect(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = Effects.DealDamage(
                    DynamicAmounts.sourcePower(),
                    EffectTarget.ContextTarget(0)
                ),
                targetRequirements = listOf(TargetOther(baseRequirement = AnyTarget())),
                descriptionOverride = "{T}: Iron Fist deals damage equal to his power to any " +
                    "other target"
            ),
            target = EffectTarget.Self
        )
        description = "Whenever you cast a spell that targets a creature you control, Iron Fist " +
            "gains \"{T}: Iron Fist deals damage equal to his power to any other target\" until " +
            "end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Erikas Perl"
        flavorText = "Danny Rand plunged his fists into the molten heart of the dragon, again " +
            "and again, until they glowed and smoldered like hot iron."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cc2d948-ac8b-4466-b604-3fabc0ab6bb9.jpg?1783902929"
    }
}
