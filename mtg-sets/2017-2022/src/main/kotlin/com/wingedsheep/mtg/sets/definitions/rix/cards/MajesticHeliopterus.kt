package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Majestic Heliopterus
 * {3}{W}
 * Creature — Dinosaur
 * 2/2
 *
 * Flying
 * Whenever this creature attacks, another target Dinosaur you control gains flying until end of turn.
 */
val MajesticHeliopterus = card("Majestic Heliopterus") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever this creature attacks, another target Dinosaur you control gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.Permanent.withSubtype("Dinosaur").youControl().other())
        effect = Effects.GrantKeyword(Keyword.FLYING, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Filip Burburan"
        flavorText = "\"Its rise is like the sun's, a beautiful beginning. Its descent is like the sun's, a frightful ending.\"\n—Huatli"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a012af0-c54e-4658-8a3b-b81cc8835e1b.jpg?1783935338"
    }
}
