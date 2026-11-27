package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thatcher Revolt — Avacyn Restored #158
 * {2}{R} · Sorcery · Common
 *
 * Create three 1/1 red Human creature tokens with haste. Sacrifice those tokens at the
 * beginning of the next end step.
 *
 * `sacrificeAtStep = Step.END` schedules the delayed sacrifice for exactly the created tokens
 * at the next end step (the Salt Road Skirmish / Hungry for More shape).
 */
val ThatcherRevolt = card("Thatcher Revolt") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create three 1/1 red Human creature tokens with haste. Sacrifice those tokens at " +
        "the beginning of the next end step."

    spell {
        effect = Effects.CreateToken(
            count = 3,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Human"),
            keywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END,
            imageUri = "https://cards.scryfall.io/normal/front/5/d/5da1ef9d-878a-49d8-afe5-86ad18e30c3d.jpg?1783940637"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Ryan Pancoast"
        flavorText = "The dark times left the peasantry ready to fight with whatever weapons were at hand."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea28bd68-47a8-47c6-be16-75ae622daf0a.jpg?1783940676"
    }
}
