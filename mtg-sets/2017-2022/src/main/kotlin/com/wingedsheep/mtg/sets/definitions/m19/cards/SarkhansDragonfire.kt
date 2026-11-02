package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sarkhan's Dragonfire
 * {3}{R}{R}
 * Sorcery
 * Sarkhan's Dragonfire deals 3 damage to any target.
 * Look at the top five cards of your library. You may reveal a red card from among them and put it
 * into your hand. Put the rest on the bottom of your library in a random order.
 *
 * The library half is [Patterns.Library.lookAtTopRevealMatchingToHand], whose defaults bottom the
 * rest in a random order. If the only target is illegal on resolution the whole spell fizzles, so
 * the library half never happens (the engine's standard fizzle rule — see the ruling below).
 */
val SarkhansDragonfire = card("Sarkhan's Dragonfire") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Sarkhan's Dragonfire deals 3 damage to any target.\n" +
        "Look at the top five cards of your library. You may reveal a red card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t) then Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Any.withColor(Color.RED),
            prompt = "You may reveal a red card from among them and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "298"
        artist = "Daarken"
        flavorText = "\"I am most myself when I am least human.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b96a7320-089a-4a7e-813f-49ca1620df76.jpg?1783934484"
        ruling("2018-07-13", "If the chosen target is an illegal target when Sarkhan's Dragonfire tries to resolve, the spell doesn't resolve. You won't look at the top five cards of your library.")
    }
}
