package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Wolf's Quarry — Throne of Eldraine #184 (canonical printing; reprinted in J22)
 * {4}{G}{G} · Sorcery
 *
 * Create three 1/1 green Boar creature tokens with "When this token dies, create a Food token."
 */
val WolfsQuarry = card("Wolf's Quarry") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create three 1/1 green Boar creature tokens with \"When this token dies, create a " +
        "Food token.\" (A Food token is an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Boar"),
            count = 3,
            triggeredAbilities = listOf(
                TriggeredAbility.create(trigger = Triggers.self.dies(), effect = Effects.CreateFood()),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/3/6/365b2234-c29d-42db-a8e0-80685a4b6434.jpg?1783932482",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Lars Grant-West"
        flavorText = "\"The monster was gaining on them. Twice it had found them. There was only one place " +
            "left to hide.\"\n—Tales of the Fae"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d21c15f-378e-4abf-992f-9743aa6ab6b8.jpg?1783932600"
        ruling("2024-11-08", "Food is an artifact type. Even though it appears on some creatures, it's never a creature type.")
    }
}
