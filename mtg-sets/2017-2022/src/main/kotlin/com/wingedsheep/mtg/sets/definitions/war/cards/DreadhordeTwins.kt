package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dreadhorde Twins
 * {3}{R}
 * Creature — Zombie Jackal Warrior
 * 2/2
 * When this creature enters, amass Zombies 2.
 * Zombie tokens you control have trample.
 *
 * "Zombie tokens" is any token with the Zombie subtype, not just the amassed Army (2023-06-16 ruling).
 */
val DreadhordeTwins = card("Dreadhorde Twins") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Zombie Jackal Warrior"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, amass Zombies 2. (Put two +1/+1 counters on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)\n" +
        "Zombie tokens you control have trample."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Amass(2, "Zombie")
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Any.withSubtype("Zombie").token().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82017bf6-352f-4e81-8fd5-c1b0daf05b01.jpg?1783933428"
        ruling(
            "2023-06-16",
            "Some cards that cause you to amass Zombies also provide bonuses to \"Zombie tokens.\" These affect any token that happens to be a Zombie, not just a Zombie Army you've amassed."
        )
    }
}
