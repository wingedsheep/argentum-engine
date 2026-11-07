package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Faithful Pikemaster — Foundations Jumpstart #3
 * {3}{W} · Creature — Rhino Monk Soldier · 3/4
 *
 * When this creature enters, scry 2.
 * As long as it's your turn, this creature has first strike.
 *
 * The first strike is Pouncing Lynx's shape: [ConditionalStaticAbility] over [GrantKeyword] on
 * [Filters.Self], gated by [Conditions.IsYourTurn].
 */
val FaithfulPikemaster = card("Faithful Pikemaster") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rhino Monk Soldier"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, scry 2. (Look at the top two cards of your library, then put any number of them on the bottom and the rest on top in any order.)\n" +
        "As long as it's your turn, this creature has first strike."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Justine Cruz"
        flavorText = "\"I seek no fight with you, but neither will I stand aside.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de149bae-dd71-4a27-a975-4d0fbc646ee4.jpg?1783908870"
    }
}
