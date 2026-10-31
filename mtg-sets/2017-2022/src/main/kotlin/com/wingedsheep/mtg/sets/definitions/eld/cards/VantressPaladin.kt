package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Vantress Paladin
 * {3}{U}
 * Creature — Human Knight
 * 2/2
 * Flying
 * Adamant — If at least three blue mana was spent to cast this spell, this creature enters with
 * a +1/+1 counter on it.
 *
 * Blue twin of Ardenvale Paladin: adamant reads [DynamicAmounts.manaOfColorSpent] against the
 * entering creature's own cast, generic included. Put onto the battlefield without being cast, it
 * spent no mana and gets no counter.
 */
val VantressPaladin = card("Vantress Paladin") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Flying\nAdamant — If at least three blue mana was spent to cast this spell, this creature enters with a +1/+1 counter on it."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(
            count = 1,
            selfOnly = true,
            condition = Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.BLUE),
                ComparisonOperator.GTE,
                3,
            ),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Lucas Graciano"
        flavorText = "\"Someone knows where the king is. They can't expect to keep it a secret from us.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52dd633f-9a07-47df-adb1-36013fc2f43a.jpg?1783932646"
    }
}
