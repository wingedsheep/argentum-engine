package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Steelgaze Griffin
 * {4}{U}
 * Creature — Griffin
 * 2/4
 * Flying
 * Whenever you draw your second card each turn, this creature gets +2/+0 until end of turn.
 */
val SteelgazeGriffin = card("Steelgaze Griffin") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Griffin"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you draw your second card each turn, this creature gets +2/+0 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "J.P. Targete"
        flavorText = "\"If we didn't guard our secrets, they wouldn't remain secrets for long.\"\n—Gadwick, the Wizened"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d361328-8b0a-40a0-b5c0-215398fdfb47.jpg?1783932649"
        ruling(
            "2019-10-04",
            "The triggered ability can trigger only once each turn. It doesn't matter whether the permanent " +
                "with that ability was on the battlefield when the first card was drawn. If it's not on the " +
                "battlefield when the second card is drawn, the ability can't trigger at all that turn. It " +
                "won't trigger when the third or fourth card is drawn."
        )
        ruling(
            "2019-10-04",
            "If an effect instructs you to draw multiple cards, the ability triggers after you draw whichever " +
                "is the second one for the turn. You choose a target (if any) for the ability after you've drawn " +
                "and looked at all of the cards and finished resolving the spell or ability that caused you to draw them."
        )
        ruling(
            "2019-10-04",
            "If a spell or ability causes you to put cards into your hand without specifically using the word " +
                "“draw,” it's not a card drawn."
        )
    }
}
