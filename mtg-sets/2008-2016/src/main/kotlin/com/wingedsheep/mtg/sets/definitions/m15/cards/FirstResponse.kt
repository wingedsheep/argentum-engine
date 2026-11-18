package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * First Response
 * {3}{W}
 * Enchantment
 *
 * At the beginning of each upkeep, if you lost life last turn, create a 1/1 white Soldier
 * creature token.
 *
 * Canonical printing: Magic 2015, the card's earliest real printing.
 *
 * "Last turn" is the previous turn in the game, whoever's it was, and only *losing* life counts —
 * the same `Conditions.YouLostLifeLastTurn` intervening-if Paladin of Atonement reads, checked
 * both when the upkeep trigger would fire and again on resolution.
 */
val FirstResponse = card("First Response") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each upkeep, if you lost life last turn, create a 1/1 white Soldier " +
        "creature token."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouLostLifeLastTurn
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Slawomir Maniak"
        flavorText = "\"There's never a good time for a disaster or an attack. That's why we're here.\"\n" +
            "—Oren, militia captain"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36341d37-064e-449e-8be7-a2716ae7e317.jpg?1783939203"
        ruling(
            "2020-11-10",
            "First Response looks at the entire previous turn to determine whether its ability triggers or not. " +
                "It doesn't matter whether First Response was on the battlefield when you lost life."
        )
        ruling(
            "2020-11-10",
            "First Response checks only if you lost life during the turn, not whether your life total decreased " +
                "over the course of the turn. For example, if you lost 2 life and then gained 8 life last turn, " +
                "First Response's ability will trigger."
        )
        ruling("2020-11-10", "Only one Soldier will be created no matter how much life was lost.")
    }
}
