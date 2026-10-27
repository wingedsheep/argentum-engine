package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * New Blood
 * {2}{B}{B}
 * Sorcery
 * As an additional cost to cast this spell, tap an untapped Vampire you control.
 * Gain control of target creature. Change the text of that creature by replacing all instances of
 * one creature type with Vampire.
 *
 * Both halves last indefinitely: the control change is permanent, and the creature-type text change
 * (fixed replacement type Vampire) ends only when the creature leaves the battlefield.
 */
val NewBlood = card("New Blood") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, tap an untapped Vampire you control.\n" +
        "Gain control of target creature. Change the text of that creature by replacing all instances of " +
        "one creature type with Vampire."

    additionalCost(
        Costs.additional.TapPermanents(count = 1, filter = GameObjectFilter.Creature.withSubtype("Vampire"))
    )

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t) then
            Effects.ChangeCreatureTypeText(target = t, replacementType = "Vampire")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Howard Lyon"
        flavorText = "\"Now isn't that better?\"\n—Olivia Voldaren"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6b44503-7978-4c9e-a8a1-fd38c76d3545.jpg?1783935944"
    }
}
