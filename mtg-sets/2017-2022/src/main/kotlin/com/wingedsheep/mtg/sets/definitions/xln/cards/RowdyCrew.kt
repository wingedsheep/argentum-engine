package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rowdy Crew
 * {2}{R}{R}
 * Creature — Human Pirate
 * 3/3
 *
 * Trample
 * When this creature enters, draw three cards, then discard two cards at random. If two cards that
 * share a card type are discarded this way, put two +1/+1 counters on this creature.
 *
 * The random discard stores the discarded cards in [Patterns.Hand.discarded]; the counters are gated
 * on [Conditions.CollectionSharesCardType] over that collection, which is false when fewer than two
 * cards were discarded (a hand that ended up with one card).
 */
val RowdyCrew = card("Rowdy Crew") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pirate"
    oracleText = "Trample\nWhen this creature enters, draw three cards, then discard two cards at random. " +
        "If two cards that share a card type are discarded this way, put two +1/+1 counters on this creature."
    power = 3
    toughness = 3

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(3) then
            Patterns.Hand.discardRandom(2) then
            Effects.If(
                condition = Conditions.CollectionSharesCardType(Patterns.Hand.discarded),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
            )
        description = "When this creature enters, draw three cards, then discard two cards at random. " +
            "If two cards that share a card type are discarded this way, put two +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "159"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdb3821a-ecc8-478d-93de-57200476868c.jpg?1783935739"
        ruling(
            "2017-09-29",
            "Once Rowdy Crew's triggered ability begins to resolve, no player may take other actions until it's done. " +
                "Notably, you can't discard or cast any of the cards you draw to try to rig the results of the random discard."
        )
        ruling(
            "2017-09-29",
            "The discarded cards just need to share one card type. For example, Rowdy Crew will get two +1/+1 counters " +
                "if you discard an artifact creature and an enchantment creature."
        )
        ruling(
            "2017-09-29",
            "Rowdy Crew doesn't get more than two +1/+1 counters if the discarded cards happen to share more than one card type."
        )
    }
}
