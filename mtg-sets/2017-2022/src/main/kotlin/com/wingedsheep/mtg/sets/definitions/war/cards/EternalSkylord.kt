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
 * Eternal Skylord — War of the Spark #49 (canonical printing)
 * {4}{U}
 * Creature — Zombie Wizard
 * 3/3
 * When this creature enters, amass Zombies 2.
 * Zombie tokens you control have flying.
 *
 * "Zombie tokens" is any token with the Zombie subtype, not just the amassed Army (2023-06-16 ruling).
 */
val EternalSkylord = card("Eternal Skylord") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Wizard"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, amass Zombies 2. (Put two +1/+1 counters on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)\n" +
        "Zombie tokens you control have flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Amass(2, "Zombie")
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.FLYING,
            GroupFilter(GameObjectFilter.Any.withSubtype("Zombie").token().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5aacfc9-1d45-4250-9abc-698d8941699f.jpg?1783933468"
        ruling(
            "2023-06-16",
            "Some cards that cause you to amass Zombies also provide bonuses to \"Zombie tokens.\" These affect any token that happens to be a Zombie, not just a Zombie Army you've amassed."
        )
    }
}
