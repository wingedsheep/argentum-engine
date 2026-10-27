package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hedron Crab — Zendikar #47 (canonical printing)
 * {U} · Creature — Crab · 0/2
 *
 * Landfall — Whenever a land you control enters, target player mills three cards.
 */
val HedronCrab = card("Hedron Crab") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Crab"
    power = 0
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, target player mills three cards. " +
        "(They put the top three cards of their library into their graveyard.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Jesper Ejsing"
        flavorText = "Hedrons perplex minds both great and small."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0fa1946-4f97-4c52-b5f2-b80571230616.jpg?1783942165"
    }
}
