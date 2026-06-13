package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Riders of the Mark
 * {6}{R}
 * Creature — Human Knight
 * 7/4
 *
 * Affinity for Humans (This spell costs {1} less to cast for each Human you control.)
 * Trample, haste
 * At the beginning of your end step, if this creature attacked this turn, return it to its owner's
 * hand. If you do, create a number of 1/1 white Human Soldier creature tokens equal to its toughness.
 *
 * Composable: the token count reads `DynamicAmounts.sourceToughness()` (last-known toughness, like
 * Heartfire Hero's source-power-on-death), evaluated after the bounce.
 */
val RidersOfTheMark = card("Riders of the Mark") {
    manaCost = "{6}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 7
    toughness = 4
    oracleText = "Affinity for Humans (This spell costs {1} less to cast for each Human you control.)\n" +
        "Trample, haste\n" +
        "At the beginning of your end step, if this creature attacked this turn, return it to its " +
        "owner's hand. If you do, create a number of 1/1 white Human Soldier creature tokens equal to " +
        "its toughness."

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.HUMAN))
    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.SourceAttackedThisTurn
        effect = Effects.ReturnToHand(EffectTarget.Self).then(
            Effects.CreateToken(
                count = DynamicAmounts.sourceToughness(),
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Human", "Soldier")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "827"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de77686e-c4c1-4d38-a05c-03eb7363fc0b.jpg?1719684208"
    }
}
