package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Selhoff Occultist
 * {2}{U}
 * Creature — Human Rogue
 * 2/3
 * Whenever this creature or another creature dies, target player mills a card.
 */
val SelhoffOccultist = card("Selhoff Occultist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    oracleText = "Whenever this creature or another creature dies, target player mills a card."
    power = 2
    toughness = 3
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(1, player)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Igor Kieryluk"
        flavorText = "\"The mist whispers to those who know it to be more than a mundane fog.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/e/aeac4885-bd04-42bd-8e10-06c3efbce108.jpg"
    }
}
