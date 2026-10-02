package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Roil Cartographer
 * {1}{U}
 * Creature — Merfolk Rogue
 * 1/3
 * Landfall — Whenever a land you control enters, you get {E} (an energy counter).
 * {T}, Pay six {E}: Draw three cards.
 */
val RoilCartographer = card("Roil Cartographer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    power = 1
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, you get {E} (an energy counter).\n" +
        "{T}, Pay six {E}: Draw three cards."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 6))
        effect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Miranda Meeks"
        flavorText = "\"The world shifts constantly, but that doesn't mean it can't be mapped. Our maps must shift as well.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84317f9d-5a24-4f74-9c3e-0a8b5c15bb5f.jpg?1783911289"

        ruling(
            "2024-11-08",
            "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land."
        )
    }
}
