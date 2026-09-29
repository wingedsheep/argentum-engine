package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Ox of Agonas
 * {3}{R}{R}
 * Creature — Ox
 * 4/2
 * When this creature enters, discard your hand, then draw three cards.
 * Escape—{R}{R}, Exile eight other cards from your graveyard.
 * This creature escapes with a +1/+1 counter on it.
 *
 * "Escapes with a +1/+1 counter" (CR 702.138c) is "if this permanent escaped, it enters with a
 * +1/+1 counter" — an [EntersWithCounters] replacement gated on [Conditions.Escaped].
 */
val OxOfAgonas = card("Ox of Agonas") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ox"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, discard your hand, then draw three cards.\n" +
        "Escape—{R}{R}, Exile eight other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Hand.discardHand() then Effects.DrawCards(3)
        description = "When this creature enters, discard your hand, then draw three cards."
    }

    keywordAbility(KeywordAbility.escape("{R}{R}", Costs.additional.ExileOtherCards(8)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "147"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75caefa7-94d5-472e-a540-daad3ee69899.jpg?1783931548"

        ruling("2020-01-24", "You draw three cards, even if you discard no cards.")
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later."
        )
    }
}
