package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Warped Tusker — Modern Horizons 3 #16 (common)
 * {7} · Creature — Eldrazi Boar Beast · 6/8
 *
 * Reach
 * When you cast or cycle Warped Tusker, create a 0/1 colorless Eldrazi Spawn creature token with
 * "Sacrifice this token: Add {C}."
 * Cycling {2}{G}
 *
 * "Cast or cycle" is two events that can never coincide, so it is two triggers sharing one effect.
 */
val WarpedTusker = card("Warped Tusker") {
    manaCost = "{7}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Boar Beast"
    power = 6
    toughness = 8
    oracleText = "Reach\nWhen you cast or cycle Warped Tusker, create a 0/1 colorless Eldrazi Spawn " +
        "creature token with \"Sacrifice this token: Add {C}.\"\nCycling {2}{G} ({2}{G}, Discard this card: Draw a card.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.CreateEldraziSpawn()
    }

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        effect = Effects.CreateEldraziSpawn()
    }

    keywordAbility(KeywordAbility.cycling("{2}{G}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Camille Alquier"
        flavorText = "Once, foraging for truffles, the very average boar fell asleep beneath the moon . . . ."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b5f3a1a-8514-4598-be3d-c3be719f6951.jpg?1783911305"
        ruling("2024-06-07", "The triggered ability will resolve before Warped Tusker or its cycling ability does, as appropriate. If Warped Tusker or its cycling ability are countered or otherwise leave the stack in response to that triggered ability, the triggered ability will still resolve as normal.")
    }
}
