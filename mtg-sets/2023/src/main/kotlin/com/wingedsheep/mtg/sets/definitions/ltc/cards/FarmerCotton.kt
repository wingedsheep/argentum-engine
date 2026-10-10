package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Farmer Cotton
 * {X}{G}{W}
 * Legendary Creature — Halfling Peasant
 * 1/1
 * When this creature enters, create X 1/1 white Halfling creature tokens and X Food tokens.
 *
 * X is the cast-time value stamped onto the enters event; it is 0 when Farmer Cotton enters
 * without being cast (the rule for X in an enters ability).
 */
val FarmerCotton = card("Farmer Cotton") {
    manaCost = "{X}{G}{W}"
    typeLine = "Legendary Creature — Halfling Peasant"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, create X 1/1 white Halfling creature tokens and X Food tokens. " +
        "(They're artifacts with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Halfling"),
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bbd9ff58-4d38-4d34-9dfc-6f788830297e.jpg?1783916062"
        ) then Effects.CreateFood(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Tomas Duchek"
        flavorText = "\"So it's begun at last! I've been itching for trouble all this year.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25b49eaa-13e7-4f4e-b0dc-56ebd28a4a22.jpg?1783916019"
    }
}
