package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Phyrexian Ironworks
 * {2}{R}
 * Artifact
 * Whenever you attack, you get {E} (an energy counter).
 * {T}, Pay {E}{E}{E}: Create a 3/3 colorless Phyrexian Golem artifact creature token.
 * Activate only as a sorcery.
 */
val PhyrexianIronworks = card("Phyrexian Ironworks") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "Whenever you attack, you get {E} (an energy counter).\n" +
        "{T}, Pay {E}{E}{E}: Create a 3/3 colorless Phyrexian Golem artifact creature token. " +
        "Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.you.attacks()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 3))
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Phyrexian", "Golem"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/4/e/4ee5119d-f76c-44c7-bf82-34aabb4d2e69.jpg?1783911109"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Mathias Kollros"
        flavorText = "\"We have made a friend today.\"\n—Slobad, Iron Goblin"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d64e1a8-cb4f-4968-b3e0-c9ca7c7894a3.jpg?1783911269"
    }
}
