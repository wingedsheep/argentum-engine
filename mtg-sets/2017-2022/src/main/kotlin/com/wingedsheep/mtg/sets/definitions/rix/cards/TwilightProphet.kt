package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Twilight Prophet — Rivals of Ixalan #88
 * {2}{B}{B} · Creature — Vampire Cleric · 2/4
 *
 * Flying
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * At the beginning of your upkeep, if you have the city's blessing, reveal the top card of your
 * library and put it into your hand. Each opponent loses X life and you gain X life, where X is
 * that card's mana value.
 *
 * Ascend is the keyword alone — the engine's state-based check grants the blessing (as on Wayward
 * Swordtooth). The upkeep trigger checks the blessing as an intervening "if", both when the step
 * begins and on resolution. X is the revealed card's mana value read from the pipeline's
 * collection (the Dark Confidant / Caustic Bronco shape); an empty library reveals nothing and X is 0.
 */
val TwilightProphet = card("Twilight Prophet") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "At the beginning of your upkeep, if you have the city's blessing, reveal the top card of your library " +
        "and put it into your hand. Each opponent loses X life and you gain X life, where X is that card's mana value."

    keywords(Keyword.FLYING, Keyword.ASCEND)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouHaveCitysBlessing
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You))
            toHand(revealed, revealed = true)
            run(
                Effects.LoseLife(
                    DynamicAmounts.manaValueOf(revealed),
                    EffectTarget.PlayerRef(Player.EachOpponent)
                )
            )
            run(Effects.GainLife(DynamicAmounts.manaValueOf(revealed)))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "88"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9da6974f-6753-48ac-98cd-86412cc93726.jpg?1783935305"
        ruling("2018-01-19", "If the mana cost of the revealed card includes {X}, X is considered to be 0.")
        ruling("2018-01-19", "If the revealed card doesn't have a mana cost (because it's a land card, for example), its mana value is 0.")
        ruling("2018-01-19", "In a Two-Headed Giant game, Twilight Prophet's last ability causes the opposing team to lose twice X life and you gain X life.")
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
        ruling(
            "2018-01-19",
            "Some cards have triggered abilities with an intervening \"if\" clause that checks whether you have the city's blessing. " +
                "You must already have the city's blessing in order for these abilities to trigger; otherwise they do nothing."
        )
    }
}
