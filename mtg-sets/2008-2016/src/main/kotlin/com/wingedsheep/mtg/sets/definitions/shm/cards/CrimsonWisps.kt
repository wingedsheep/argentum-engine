package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crimson Wisps
 * {R}
 * Instant
 * Target creature becomes red and gains haste until end of turn.
 * Draw a card.
 */
val CrimsonWisps = card("Crimson Wisps") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature becomes red and gains haste until end of turn. (It can attack and {T} this turn.)\nDraw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ChangeColor(t, setOf(Color.RED)) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Jim Nelson"
        flavorText = "The whispers are incomprehensible, yet cinders always hear a message of urgency."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a65c81ff-fc5d-4191-93fb-52eb806457b7.jpg?1783942749"
        ruling(
            "2008-05-01",
            "An effect that changes a permanent's colors overwrites all its old colors unless it specifically says \"in addition to its other colors.\" For example, after Cerulean Wisps resolves, the affected creature will just be blue. It doesn't matter what colors it used to be (even if, for example, it used to be blue and black)."
        )
        ruling(
            "2008-05-01",
            "Changing a permanent's color won't change its text. If you turn Wilt-Leaf Liege blue, it will still affect green creatures and white creatures."
        )
    }
}
