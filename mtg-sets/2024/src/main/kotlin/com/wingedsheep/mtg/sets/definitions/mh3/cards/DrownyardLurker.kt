package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Drownyard Lurker — Modern Horizons 3 #3 (common)
 * {7} · Creature — Eldrazi Trilobite · 7/7
 *
 * Vigilance
 * When you cast or cycle Drownyard Lurker, create a 0/1 colorless Eldrazi Spawn creature token with
 * "Sacrifice this token: Add {C}."
 * Cycling {2}{U}
 *
 * "Cast or cycle" is two events that can never coincide, so it is two triggers sharing one effect:
 * a cast trigger (`Triggers.self.isCast()`) and a cycle trigger (`Triggers.self.isCycled()`).
 */
val DrownyardLurker = card("Drownyard Lurker") {
    manaCost = "{7}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Trilobite"
    power = 7
    toughness = 7
    oracleText = "Vigilance\nWhen you cast or cycle Drownyard Lurker, create a 0/1 colorless Eldrazi Spawn " +
        "creature token with \"Sacrifice this token: Add {C}.\"\nCycling {2}{U} ({2}{U}, Discard this card: Draw a card.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.CreateEldraziSpawn()
    }

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        effect = Effects.CreateEldraziSpawn()
    }

    keywordAbility(KeywordAbility.cycling("{2}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Loïc Canavaggia"
        flavorText = "When Emrakul arrived on Innistrad, even fossils answered her twisted call."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7faf5812-2067-4386-bb83-150723d67d02.jpg?1783911309"
        ruling("2024-06-07", "Drownyard Lurker's triggered ability will resolve before either Drownyard Lurker or its cycling ability does, as appropriate. If Drownyard Lurker or its cycling ability are countered or otherwise leave the stack in response to that triggered ability, the triggered ability will still resolve as normal.")
    }
}
