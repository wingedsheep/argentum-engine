package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bladecoil Serpent
 * {X}{6}
 * Artifact Creature — Serpent
 * 5/4
 * When this creature enters, for each {U}{U} spent to cast it, draw a card.
 * When this creature enters, for each {B}{B} spent to cast it, each opponent discards a card.
 * When this creature enters, for each {R}{R} spent to cast it, it gets +1/+0 and gains trample
 * and haste until end of turn.
 */
private val bluePairs = DynamicAmounts.manaOfColorSpent(Color.BLUE) / 2
private val blackPairs = DynamicAmounts.manaOfColorSpent(Color.BLACK) / 2
private val redPairs = DynamicAmounts.manaOfColorSpent(Color.RED) / 2

val BladecoilSerpent = card("Bladecoil Serpent") {
    manaCost = "{X}{6}"
    colorIdentity = "UBR"
    typeLine = "Artifact Creature — Serpent"
    power = 5
    toughness = 4
    oracleText = "When this creature enters, for each {U}{U} spent to cast it, draw a card.\n" +
        "When this creature enters, for each {B}{B} spent to cast it, each opponent discards a card.\n" +
        "When this creature enters, for each {R}{R} spent to cast it, it gets +1/+0 and gains trample and haste until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(bluePairs)
        description = "When this creature enters, for each {U}{U} spent to cast it, draw a card."
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.EachOpponentDiscards(blackPairs)
        description = "When this creature enters, for each {B}{B} spent to cast it, each opponent discards a card."
    }

    // Trample and haste are granted once however many {R}{R} were spent, and not at all for none.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            Conditions.CompareAmounts(redPairs, ComparisonOperator.GTE, 1),
            Effects.ModifyStats(redPairs, DynamicAmounts.fixed(0), EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
        )
        description = "When this creature enters, for each {R}{R} spent to cast it, it gets +1/+0 and gains trample and haste until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "229"
        artist = "Joshua Cairos"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08c5a528-d16d-4cb9-b532-b85648c8395a.jpg?1783920022"
    }
}
