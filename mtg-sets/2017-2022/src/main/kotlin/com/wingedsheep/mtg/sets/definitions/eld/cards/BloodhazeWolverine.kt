package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodhaze Wolverine
 * {1}{R}
 * Creature — Wolverine
 * 2/1
 * Whenever you draw your second card each turn, this creature gets +1/+1 and gains first strike
 * until end of turn.
 */
val BloodhazeWolverine = card("Bloodhaze Wolverine") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolverine"
    power = 2
    toughness = 1
    oracleText = "Whenever you draw your second card each turn, this creature gets +1/+1 and gains first strike until end of turn."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Evan Shipard"
        flavorText = "\"Build a moat. If we're lucky, it can't swim.\"\n—Syr Branigan, knight of Ardenvale"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d6c0bc1-cb07-4009-83b1-1122381cf9c4.jpg?1783932628"
        ruling(
            "2019-10-04",
            "If Bloodhaze Wolverine gains first strike after first-strike combat damage has been dealt, " +
                "it will just deal regular combat damage."
        )
        ruling(
            "2019-10-04",
            "The triggered ability can trigger only once each turn. It doesn't matter whether the permanent " +
                "with that ability was on the battlefield when the first card was drawn. If it's not on the " +
                "battlefield when the second card is drawn, the ability can't trigger at all that turn. It " +
                "won't trigger when the third or fourth card is drawn."
        )
        ruling(
            "2019-10-04",
            "If a spell or ability causes you to put cards into your hand without specifically using the word " +
                "\"draw,\" it's not a card drawn."
        )
    }
}
