package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Magmaquake — Magic 2013 #140
 * {X}{R}{R} · Instant
 *
 * Magmaquake deals X damage to each creature without flying and each planeswalker.
 *
 * One union filter, so a permanent that is both a creature and a planeswalker is dealt X damage
 * once — and a flying creature planeswalker is still hit as a planeswalker. Nothing is targeted.
 */
val Magmaquake = card("Magmaquake") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Magmaquake deals X damage to each creature without flying and each planeswalker."

    spell {
        effect = Patterns.Group.dealDamageToAll(
            DynamicAmounts.xValue(),
            GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING) or GameObjectFilter.Planeswalker)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Gabor Szikszai"
        flavorText = "\"Where will you run when I punish you with the very ground you flee on?\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac85679e-17c7-4525-8eed-979d04feb8f1.jpg?1783940481"
        ruling(
            "2012-07-01",
            "Magmaquake doesn't target any creature or planeswalker. For example, it will deal damage to a " +
                "creature without flying that has hexproof."
        )
    }
}
