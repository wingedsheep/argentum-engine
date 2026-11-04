package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kumena's Awakening — Rivals of Ixalan #42
 * {2}{U}{U} · Enchantment
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * At the beginning of your upkeep, each player draws a card. If you have the city's blessing,
 * instead only you draw a card.
 *
 * Ascend on a permanent is the keyword alone (the engine's state-based check grants the blessing).
 * The upkeep trigger has no intervening "if": it always triggers and checks the blessing only as it
 * resolves, choosing between "only you draw" and "each player draws" (the Golden Demise shape).
 */
val KumenasAwakening = card("Kumena's Awakening") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "At the beginning of your upkeep, each player draws a card. If you have the city's blessing, " +
        "instead only you draw a card."

    keywords(Keyword.ASCEND)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.YouHaveCitysBlessing,
            then = Effects.DrawCards(1),
            otherwise = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a86f499-a2d4-47f1-9fc1-b543e5ad5c82.jpg?1783935323"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
        ruling("2018-01-19", "A permanent is any object on the battlefield, including tokens and lands. Spells and emblems aren't permanents.")
        ruling(
            "2018-01-19",
            "Ascend on a permanent isn't a triggered ability and doesn't use the stack. Players can respond to a spell that will give you your " +
                "tenth permanent, but they can't respond to getting the city's blessing once you control that tenth permanent. This means that " +
                "if your tenth permanent is a land you play, players can't respond before you get the city's blessing."
        )
        ruling(
            "2018-01-19",
            "Some cards have triggered abilities that check if you have the city's blessing, but don't use an intervening \"if\" clause. " +
                "These abilities trigger regardless of whether you have the city's blessing and check whether you do only as they resolve."
        )
    }
}
