package com.wingedsheep.mtg.sets.definitions.eoc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersUntapped
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Horizon Explorer — Edge of Eternities Commander #15 (canonical printing)
 * {2}{G} · Creature — Insect Scout · 2/4
 *
 * Lands you control enter untapped.
 * Whenever you attack a player, create a Lander token.
 *
 * The Wandering Minstrel's [EntersUntapped] static replacement, plus the once-per-attacked-player
 * `Triggers.you.attacksAPlayer()` (the ruling: one trigger for each player you attack).
 */
val HorizonExplorer = card("Horizon Explorer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Scout"
    power = 2
    toughness = 4
    oracleText = "Lands you control enter untapped.\n" +
        "Whenever you attack a player, create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice " +
        "this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    replacementEffect(
        EntersUntapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Land.youControl(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.you.attacksAPlayer()
        effect = Effects.CreateLander()
        description = "Whenever you attack a player, create a Lander token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5873a4d-ab6e-4dd2-9380-5eaa367a8396.jpg?1783906062"

        ruling(
            "2025-07-25",
            "If a land has an ability that says it enters tapped, you choose the order in which that ability's " +
                "effect and the effect of Horizon Explorer's first ability apply. This means you can choose to have " +
                "the land enter tapped or untapped. If a land you control is simply put onto the battlefield tapped " +
                "without a replacement effect being applied, it always enters untapped if you control Horizon Explorer."
        )
        ruling("2025-07-25", "Horizon Explorer's last ability will trigger once for each player you attack.")
    }
}
