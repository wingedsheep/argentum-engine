package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Agent of Atlas
 * {1}{W}
 * Creature — Human Spy Hero
 * 2/2
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 *
 * Implementation note: vanilla body plus `prowess()`, the CardBuilder helper that adds both the
 * keyword (display) and the intrinsic "+1/+1 until end of turn on a noncreature cast" triggered
 * ability. `keywords(Keyword.PROWESS)` alone would render the keyword but never fire.
 */
val AgentOfAtlas = card("Agent of Atlas") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Spy Hero"
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)"
    power = 2
    toughness = 2
    prowess()
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Michael Machira"
        flavorText = "\"I don't do 'conquest.' The Atlas Foundation is now a force for peace.\"\n—Jimmy Woo"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9db550e-ed1f-4973-a7f5-ec2e6adb8e6b.jpg?1783902979"
    }
}
