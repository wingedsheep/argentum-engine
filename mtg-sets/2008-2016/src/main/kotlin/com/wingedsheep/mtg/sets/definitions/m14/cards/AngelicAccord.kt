package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Angelic Accord — Magic 2014 #3
 * {3}{W} · Enchantment
 *
 * At the beginning of each end step, if you gained 4 or more life this turn, create a 4/4 white
 * Angel creature token with flying.
 *
 * Intervening "if": counts total life gained this turn (not net), checked as the end step begins
 * and again on resolution — Valkyrie Harbinger's trigger without the vigilance.
 */
val AngelicAccord = card("Angelic Accord") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each end step, if you gained 4 or more life this turn, create a " +
        "4/4 white Angel creature token with flying."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(4)
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/8/2/826320b7-4ccf-4ace-8c2c-15052b6bad73.jpg?1783939887"
        )
        description = "At the beginning of each end step, if you gained 4 or more life this turn, create " +
            "a 4/4 white Angel creature token with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Michael C. Hayes"
        flavorText = "As demons are drawn to suffering, angels are drawn to acts of benevolence."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03f26bc2-53d7-4448-8021-de35aa82fcc6.jpg?1783939947"
        ruling("2017-11-17", "Angelic Accord's ability checks how much life you've gained during the turn, not what your life total is compared to what it was when the turn began. For example, if you start the turn at 10 life, gain 6 life during the turn, then lose 6 life later that turn, the ability will trigger.")
        ruling("2017-11-17", "If you haven't gained 4 or more life during the turn when the end step begins, the ability won't trigger at all. Gaining life during the end step won't cause the ability to trigger.")
    }
}
