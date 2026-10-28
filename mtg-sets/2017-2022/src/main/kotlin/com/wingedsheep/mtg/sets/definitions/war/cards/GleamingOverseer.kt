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
 * Gleaming Overseer
 * {1}{U}{B}
 * Creature — Zombie Wizard
 * 1/4
 * When this creature enters, amass Zombies 1.
 * Zombie tokens you control have hexproof and menace.
 *
 * "Zombie tokens" is any token with the Zombie subtype, not just the amassed Army (2023-06-16 ruling).
 */
private val zombieTokensYouControl =
    GroupFilter(GameObjectFilter.Any.withSubtype("Zombie").token().youControl())

val GleamingOverseer = card("Gleaming Overseer") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Wizard"
    power = 1
    toughness = 4
    oracleText = "When this creature enters, amass Zombies 1. (Put a +1/+1 counter on an Army you control. " +
        "It's also a Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)\n" +
        "Zombie tokens you control have hexproof and menace."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Amass(1, "Zombie")
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, zombieTokensYouControl)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, zombieTokensYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b4604b4-df53-485c-a604-4d823b33d8ba.jpg?1783933397"
        ruling(
            "2023-06-16",
            "Some cards that cause you to amass Zombies also provide bonuses to \"Zombie tokens.\" These affect any token that happens to be a Zombie, not just a Zombie Army you've amassed."
        )
    }
}
