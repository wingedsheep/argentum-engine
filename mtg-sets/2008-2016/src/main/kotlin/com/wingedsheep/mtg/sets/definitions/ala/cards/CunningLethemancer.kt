package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cunning Lethemancer — Shards of Alara #69 (canonical printing)
 * {2}{B} · Creature — Human Wizard · 2/2
 *
 * At the beginning of your upkeep, each player discards a card.
 *
 * Symmetric — the Lethemancer's controller discards too — so it's [Effects.EachPlayerDiscards].
 */
val CunningLethemancer = card("Cunning Lethemancer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your upkeep, each player discards a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.EachPlayerDiscards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Paul Bonner"
        flavorText = "\"Give me your memories, and you will have release from the suffering they bring you.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/972891e6-6a23-40df-af22-adf8cd7ea582.jpg?1783942568"
    }
}
