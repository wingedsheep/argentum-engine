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
 * Vizier of the Scorpion
 * {2}{B}
 * Creature — Zombie Wizard
 * 1/1
 * When this creature enters, amass Zombies 1.
 * Zombie tokens you control have deathtouch.
 *
 * "Zombie tokens" is any token with the Zombie subtype, not just the amassed Army (2023-06-16 ruling).
 */
val VizierOfTheScorpion = card("Vizier of the Scorpion") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Wizard"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, amass Zombies 1. (Put a +1/+1 counter on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)\n" +
        "Zombie tokens you control have deathtouch."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Amass(1, "Zombie")
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.DEATHTOUCH,
            GroupFilter(GameObjectFilter.Any.withSubtype("Zombie").token().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Zack Stella"
        flavorText = "His blessing is a curse."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2444c7dc-7aa1-4476-8660-19d26607306d.jpg?1783933436"
        ruling(
            "2023-06-16",
            "Some cards that cause you to amass Zombies also provide bonuses to \"Zombie tokens.\" These affect any token that happens to be a Zombie, not just a Zombie Army you've amassed."
        )
    }
}
