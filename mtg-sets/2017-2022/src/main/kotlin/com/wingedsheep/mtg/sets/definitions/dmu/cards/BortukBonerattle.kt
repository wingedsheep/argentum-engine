package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bortuk Bonerattle
 * {4}{B}{G}
 * Legendary Creature — Troll Shaman
 * 4/4
 * Domain — When Bortuk Bonerattle enters, if you cast it, choose target creature card in your
 * graveyard. Return that card to the battlefield if its mana value is less than or equal to the
 * number of basic land types among lands you control. Otherwise, put it into your hand.
 *
 * `Conditions.WasCast` is the intervening-if (any zone; not when put onto the battlefield without
 * casting). The mana value / domain comparison is made on resolution, with both sides read live:
 * the target's mana value off its card in the graveyard and the domain count off projected land
 * subtypes. A target that left the graveyard makes the single-target ability fizzle (CR 608.2b).
 */
val BortukBonerattle = card("Bortuk Bonerattle") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Troll Shaman"
    power = 4
    toughness = 4
    oracleText = "Domain — When Bortuk Bonerattle enters, if you cast it, choose target creature card in your graveyard. " +
        "Return that card to the battlefield if its mana value is less than or equal to the number of basic land types " +
        "among lands you control. Otherwise, put it into your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaValueOf(creatureCard),
                ComparisonOperator.LTE,
                DynamicAmounts.domain()
            ),
            then = Effects.PutOntoBattlefieldFromGraveyard(creatureCard),
            otherwise = Effects.ReturnToHand(creatureCard)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a25855f-abfc-482d-a36b-088b023a7df6.jpg?1783921286"
        ruling("2022-09-09", "The basic land types are Plains, Island, Swamp, Mountain, and Forest. Land types other than basic land types (such as Desert) don't contribute to domain abilities.")
    }
}
