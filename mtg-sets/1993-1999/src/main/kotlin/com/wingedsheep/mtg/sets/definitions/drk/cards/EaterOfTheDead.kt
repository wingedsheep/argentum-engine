package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eater of the Dead
 * {4}{B}
 * Creature — Horror
 * 3/4
 * {0}: If this creature is tapped, exile target creature card from a graveyard and untap this
 * creature.
 *
 * The "if this creature is tapped" clause is a **resolution-time** check, not an activation
 * restriction — the ability can legally be activated (and targeted) while the Eater is untapped, it
 * just does nothing. So it is a `ConditionalEffect` on `SourceIsTapped` rather than a
 * `TimingRule.OnlyIfCondition` or an `ActivationRestriction`, which would wrongly hide the ability
 * from the untapped Eater's controller.
 *
 * Any graveyard, not just yours — the card says "a graveyard".
 *
 * The famous consequence is left intact: with the Eater tapped and creature cards in graveyards,
 * the {0} cost makes this repeatable at no cost, which is exactly what the card does.
 */
val EaterOfTheDead = card("Eater of the Dead") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 3
    toughness = 4
    oracleText = "{0}: If this creature is tapped, exile target creature card from a graveyard " +
        "and untap this creature."

    activatedAbility {
        cost = Costs.Free
        target = Targets.CreatureCardInGraveyard
        effect = ConditionalEffect(
            condition = Conditions.SourceIsTapped,
            effect = Effects.Composite(
                Effects.Exile(EffectTarget.ContextTarget(0), fromZone = Zone.GRAVEYARD),
                Effects.Untap(EffectTarget.Self),
            ),
        )
        description = "{0}: If this creature is tapped, exile target creature card from a " +
            "graveyard and untap this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Jesper Myrfors"
        flavorText = "Even the putrid muscles of the dead can provide strength to those loathsome " +
            "enough to consume them."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d89fe2be-bb7e-4bae-9b1f-9f0d58f20ceb.jpg?1783947940"
    }
}
