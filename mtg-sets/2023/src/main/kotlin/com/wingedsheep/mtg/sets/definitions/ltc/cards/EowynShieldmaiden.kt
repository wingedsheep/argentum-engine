package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Éowyn, Shieldmaiden
 * {2}{U}{R}{W}
 * Legendary Creature — Human Knight
 * 5/4
 *
 * First strike
 * At the beginning of combat on your turn, if another Human entered the battlefield under your
 * control this turn, create two 2/2 red Human Knight creature tokens with trample and haste. Then
 * if you control six or more Humans, draw a card.
 *
 * - "Another Human" is turn history read with `excludeSource`: Éowyn's own entry doesn't count,
 *   but a Human that entered and has since left still does.
 * - Intervening "if" (CR 603.4): with no other Human entry the ability doesn't trigger at all, so
 *   there's no draw even with six Humans (ruling).
 */
val EowynShieldmaiden = card("Éowyn, Shieldmaiden") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Human Knight"
    power = 5
    toughness = 4
    oracleText = "First strike\n" +
        "At the beginning of combat on your turn, if another Human entered the battlefield under " +
        "your control this turn, create two 2/2 red Human Knight creature tokens with trample and " +
        "haste. Then if you control six or more Humans, draw a card."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.subtypeEnteredUnderControlThisTurn(Subtype.HUMAN, excludeSource = true),
            ComparisonOperator.GTE,
            1
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Human", "Knight"),
            keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/4/9/491fc1c3-a46e-4cfd-a749-57f4c96f6aea.jpg?1783916057"
        ) then Effects.If(
            Conditions.YouControlAtLeast(6, GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN)),
            Effects.DrawCards(1)
        )
        description = "At the beginning of combat on your turn, if another Human entered the " +
            "battlefield under your control this turn, create two 2/2 red Human Knight creature " +
            "tokens with trample and haste. Then if you control six or more Humans, draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Craig Elliott"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd4a00ff-2206-4e12-a0ab-61ed82c9e6c5.jpg?1783916039"
    }
}
