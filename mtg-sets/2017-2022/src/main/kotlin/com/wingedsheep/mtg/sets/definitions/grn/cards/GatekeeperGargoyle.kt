package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gatekeeper Gargoyle
 * {6}
 * Artifact Creature — Gargoyle
 * 3/3
 * Flying
 * This creature enters with a +1/+1 counter on it for each Gate you control.
 */
val GatekeeperGargoyle = card("Gatekeeper Gargoyle") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Gargoyle"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "This creature enters with a +1/+1 counter on it for each Gate you control."

    keywords(Keyword.FLYING)

    // "This creature enters with a +1/+1 counter on it for each Gate you control."
    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Gate")).count()
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Adam Paquette"
        flavorText = "Each night it visits a different guildgate around the city, digging its talons into fresh stone."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bbdd54e-5d7e-4e4b-8d59-4e3f46877d39.jpg?1783934109"
    }
}
