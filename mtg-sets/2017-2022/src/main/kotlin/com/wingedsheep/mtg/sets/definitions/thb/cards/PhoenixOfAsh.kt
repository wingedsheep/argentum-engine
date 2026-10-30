package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phoenix of Ash
 * {1}{R}{R}
 * Creature — Phoenix
 * 2/2
 * Flying, haste
 * {2}{R}: This creature gets +2/+0 until end of turn.
 * Escape—{2}{R}{R}, Exile three other cards from your graveyard.
 * This creature escapes with a +1/+1 counter on it.
 *
 * Escape and "escapes with a +1/+1 counter" are Ox of Agonas's shape: [KeywordAbility.escape] plus
 * an [EntersWithCounters] replacement gated on [Conditions.Escaped] (CR 702.138c).
 */
val PhoenixOfAsh = card("Phoenix of Ash") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 2
    toughness = 2
    oracleText = "Flying, haste\n" +
        "{2}{R}: This creature gets +2/+0 until end of turn.\n" +
        "Escape—{2}{R}{R}, Exile three other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)\n" +
        "This creature escapes with a +1/+1 counter on it."

    keywords(Keyword.FLYING, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "This creature gets +2/+0 until end of turn."
    }

    keywordAbility(KeywordAbility.escape("{2}{R}{R}", Costs.additional.ExileOtherCards(3)))

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true,
        condition = Conditions.Escaped
    ))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7010ce26-16de-44cb-ba7e-3d904175b429.jpg?1783931546"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
        ruling(
            "2020-01-24",
            "If a card with escape is put into your graveyard during your turn, you'll be able to cast it right " +
                "away if it's legal to do so, before an opponent can take any actions."
        )
    }
}
