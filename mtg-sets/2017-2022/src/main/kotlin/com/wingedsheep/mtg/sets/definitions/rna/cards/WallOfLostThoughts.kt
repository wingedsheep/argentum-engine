package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Lost Thoughts — Ravnica Allegiance #59
 * {1}{U} · Creature — Wall · 0 / 4
 */
val WallOfLostThoughts = card("Wall of Lost Thoughts") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 4
    oracleText = "Defender (This creature can't attack.)\n" +
        "When this creature enters, target player mills four cards. " +
        "(They put the top four cards of their library into their graveyard.)"

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val p = target(Targets.Player)
        effect = Patterns.Library.mill(4, p)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Adam Paquette"
        flavorText = "Those who intrude upon the Dimir seldom remember that they have done so."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03a0a627-ea7a-48bb-bf30-60b2677dd8ae.jpg?1783933701"
    }
}
