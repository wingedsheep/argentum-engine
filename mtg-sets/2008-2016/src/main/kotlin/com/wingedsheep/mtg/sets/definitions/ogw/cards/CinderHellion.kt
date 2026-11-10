package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cinder Hellion
 * {4}{R}
 * Creature — Hellion
 * 4/4
 * Trample
 * When this creature enters, it deals 2 damage to target opponent or planeswalker.
 */
val CinderHellion = card("Cinder Hellion") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Hellion"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "When this creature enters, it deals 2 damage to target opponent or planeswalker."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(2, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Jason Kang"
        flavorText = "Chandra used jets of flame to lure hellions into the path of oncoming Eldrazi."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a069118-42e4-40e3-a2ba-593ac89b9064.jpg"
    }
}
