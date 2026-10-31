package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fire Prophecy
 * {1}{R}
 * Instant
 * Fire Prophecy deals 3 damage to target creature. You may put a card from your hand on the
 * bottom of your library. If you do, draw a card.
 *
 * Same shape as Volcanic Spite: the optional tuck is `chooseUpTo(1)` from hand and
 * [Effects.IfYouDo] draws only when a card actually moved.
 */
val FireProphecy = card("Fire Prophecy") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Fire Prophecy deals 3 damage to target creature. " +
        "You may put a card from your hand on the bottom of your library. If you do, draw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, t) then Effects.IfYouDo(
            action = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val chosen = chooseUpTo(1, from = hand, prompt = "You may put a card from your hand on the bottom of your library")
                toLibraryBottom(chosen)
            },
            then = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Kieran Yanner"
        flavorText = "\"Water and bone tell many tales, but fire is fickle. It would rather consume than reveal.\"\n—Rielle, the Everwise"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c2029e5-cf7d-461f-b7b9-bf96399d8f49.jpg?1783931051"
        ruling("2020-04-17", "If the target creature is an illegal target by the time Fire Prophecy tries to resolve, the spell won't resolve. You won't have the option to put a card on the bottom of your library.")
        ruling("2020-04-17", "While resolving Fire Prophecy, you can't put more than one card on the bottom of your library to draw more than one card.")
    }
}
