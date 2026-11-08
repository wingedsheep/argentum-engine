package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Architect of the Untamed — Kaladesh #143
 * {2}{G} · Creature — Elf Artificer Druid · 2/3
 *
 * Landfall — Whenever a land you control enters, you get {E}.
 * Pay eight {E}: Create a 6/6 colorless Beast artifact creature token.
 */
val ArchitectOfTheUntamed = card("Architect of the Untamed") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Artificer Druid"
    power = 2
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, you get {E} (an energy counter).\n" +
        "Pay eight {E}: Create a 6/6 colorless Beast artifact creature token."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 8)
        effect = Effects.CreateToken(
            power = 6,
            toughness = 6,
            colors = emptySet(),
            creatureTypes = setOf("Beast"),
            artifactToken = true,
        )
        description = "Pay eight {E}: Create a 6/6 colorless Beast artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Sara Winters"
        flavorText = "Next to her designs, life seems like a pale imitation."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bc540f5-96a1-44d8-910d-914b9e61b2a5.jpg"
    }
}
