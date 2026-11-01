package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invade the City — War of the Spark #201 (canonical printing)
 * {1}{U}{R}
 * Sorcery
 * Amass Zombies X, where X is the number of instant and sorcery cards in your graveyard.
 *
 * X is counted on resolution, while the spell is still on the stack, so it never counts itself.
 */
val InvadeTheCity = card("Invade the City") {
    manaCost = "{1}{U}{R}"
    typeLine = "Sorcery"
    oracleText = "Amass Zombies X, where X is the number of instant and sorcery cards in your graveyard. " +
        "(Put X +1/+1 counters on an Army you control. It's also a Zombie. If you don't control an Army, " +
        "create a 0/0 black Zombie Army creature token first.)"

    spell {
        effect = Effects.Amass(
            DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
            "Zombie",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Sung Choi"
        flavorText = "Death greeted the world with a roar of desert air."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a587eb67-c2df-486c-9c48-dfeac540efe8.jpg?1783933395"
        ruling("2019-05-03", "If there are no instant or sorcery cards in your graveyard, you'll amass 0. If you don't control an Army, you'll create a 0/0 black Zombie Army creature token that will die after Invade the City has finished resolving.")
        ruling("2019-05-03", "The number of instant and sorcery cards in your graveyard is counted only as Invade the City resolves.")
        ruling("2019-05-03", "A split card that's both an instant and a sorcery is counted only once for Invade the City.")
        ruling("2019-05-03", "Invade the City is still on the stack while you count your instant and sorcery cards in your graveyard. It doesn't count itself.")
    }
}
