package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Speaker of the Heavens
 * {W}
 * Creature — Human Cleric
 * 1/1
 * Vigilance, lifelink
 * {T}: Create a 4/4 white Angel creature token with flying. Activate only if you have at least
 * 7 life more than your starting life total and only as a sorcery.
 *
 * The life gate is [Conditions.LifeAboveStartingBy] (Righteous Valkyrie's "at least 7 life more
 * than your starting life total"), which reads the seat's real starting total, behind an
 * [ActivationRestriction.OnlyIfCondition]; "only as a sorcery" is [TimingRule.SorcerySpeed].
 */
val SpeakerOfTheHeavens = card("Speaker of the Heavens") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Vigilance, lifelink (Attacking doesn't cause this creature to tap. Damage dealt by this creature also causes you to gain that much life.)\n" +
        "{T}: Create a 4/4 white Angel creature token with flying. Activate only if you have at least 7 life more than your starting life total and only as a sorcery."

    keywords(Keyword.VIGILANCE, Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Angel"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/9/e/9e12d954-3ec2-46e3-b01f-1fd63159e8a4.jpg?1783930594",
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.LifeAboveStartingBy(7))
        )
        timing = TimingRule.SorcerySpeed
        description = "{T}: Create a 4/4 white Angel creature token with flying. Activate only if you have " +
            "at least 7 life more than your starting life total and only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Randy Vargas"
        flavorText = "\"All are welcome in the angels' light.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f44b96a-8498-414a-a4ac-54c80dfa9f23.jpg?1783930732"
        ruling("2020-06-23", "In a Two-Headed Giant game, you can activate the ability only if your team's life total is at least 7 more than your team's starting life total.")
    }
}
