package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Mister Fantastic, Reed Richards — Marvel Super Heroes #66
 * {3}{U} · Legendary Creature — Human Scientist Hero · 2/4
 *
 * Reach
 * Whenever one or more tokens you control enter, you may draw a card.
 *
 * The batched enters trigger is [Triggers.OneOrMorePermanentsEnter] over
 * [GameObjectFilter.Token] — the filter's controller scope defaults to "you control", and the
 * batching means five tokens entering together draw one card, not five (CR 603.3b). The "you
 * may" is [MayEffect], asked on resolution.
 */
val MisterFantasticReedRichards = card("Mister Fantastic, Reed Richards") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Scientist Hero"
    power = 2
    toughness = 4
    oracleText = "Reach\nWhenever one or more tokens you control enter, you may draw a card."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Token)
        effect = MayEffect(Effects.DrawCards(1))
        description = "Whenever one or more tokens you control enter, you may draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Rimas Valeikis"
        flavorText = "He's always in control of the situation, even when stretched thin."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17ef068b-61fc-443d-97b0-1e41f2622425.jpg?1783902954"
    }
}
