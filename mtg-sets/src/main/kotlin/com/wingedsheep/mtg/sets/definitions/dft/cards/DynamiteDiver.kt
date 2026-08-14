package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CrewSaddleContribution

/**
 * Dynamite Diver — Aetherdrift #123
 * {R} · Creature — Goblin Pilot · 1/1
 */
val DynamiteDiver = card("Dynamite Diver") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Pilot"
    oracleText = "This creature saddles Mounts and crews Vehicles as though its power were 2 greater.\n" +
        "When this creature dies, it deals 1 damage to any target."
    power = 1
    toughness = 1

    staticAbility {
        ability = CrewSaddleContribution(modifier = 2)
    }

    triggeredAbility {
        trigger = Triggers.Dies
        val damaged = target("any target", Targets.Any)
        effect = Effects.DealDamage(1, damaged)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Pete Venters"
        flavorText = "\"Launch the explosives! No, you idiot, not like that!\"\n" +
            "—Redshift, Rocketeer chief"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a4f96f6-dd91-4357-ae19-1c35db2c2bcb.jpg?1783907883"
    }
}
