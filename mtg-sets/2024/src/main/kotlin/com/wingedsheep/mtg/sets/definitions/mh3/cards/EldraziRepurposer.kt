package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Eldrazi Repurposer — Modern Horizons 3 #150 (common)
 * {2}{G} · Creature — Eldrazi Drone · 3/3
 *
 * Devoid
 * When you cast this spell and when this creature dies, create a 0/1 colorless Eldrazi Spawn
 * creature token with "Sacrifice this token: Add {C}."
 *
 * The joined "when you cast … and when … dies" is two triggered abilities sharing one effect.
 */
val EldraziRepurposer = card("Eldrazi Repurposer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "When you cast this spell and when this creature dies, create a 0/1 colorless Eldrazi " +
        "Spawn creature token with \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.CreateEldraziSpawn()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateEldraziSpawn()
    }

    metadata {
        ruling("2024-06-07", "Eldrazi Repurposer's triggered ability will resolve before Eldrazi Repurposer does. If Eldrazi Repurposer is countered or otherwise leaves the stack in response to that triggered ability, the triggered ability will still resolve as normal.")
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Daren Bader"
        flavorText = "\"I almost admire their ability to avoid waste. Almost.\"\n—General Tazri, allied commander"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37f79ba7-7b65-4387-b498-f770816ce8dd.jpg?1783911262"
    }
}
