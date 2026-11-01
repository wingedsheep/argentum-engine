package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bleeding Edge — War of the Spark #78 (canonical printing)
 * {1}{B}{B}
 * Sorcery
 * Up to one target creature gets -2/-2 until end of turn. Amass Zombies 2.
 *
 * "Up to one" is an optional target: cast with no target and you just amass 2. If a chosen target
 * becomes illegal, the spell doesn't resolve and you don't amass (2019-05-03 ruling).
 */
val BleedingEdge = card("Bleeding Edge") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Up to one target creature gets -2/-2 until end of turn. Amass Zombies 2. " +
        "(Put two +1/+1 counters on an Army you control. It's also a Zombie. " +
        "If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    spell {
        val t = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-2, -2, t) then Effects.Amass(2, "Zombie")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac82422c-8ac8-4fbb-b9b9-d0aa23dded61.jpg?1783933451"
        ruling(
            "2019-05-03",
            "You may cast Bleeding Edge without choosing a target creature. You'll just amass 2. However, if you choose a target and that target become illegal before Bleeding Edge resolves, the spell won't resolve and you won't amass."
        )
    }
}
