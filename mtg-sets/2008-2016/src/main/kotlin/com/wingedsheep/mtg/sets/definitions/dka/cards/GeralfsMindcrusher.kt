package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Geralf's Mindcrusher
 * {4}{U}{U}
 * Creature — Zombie Horror
 * 5/5
 * When this creature enters, target player mills five cards.
 * Undying
 */
val GeralfsMindcrusher = card("Geralf's Mindcrusher") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    power = 5
    toughness = 5
    oracleText = "When this creature enters, target player mills five cards.\nUndying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"

    keywords(Keyword.UNDYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(5, player)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68ac8b5f-4d95-43fc-bf23-10247986a746.jpg"
    }
}
