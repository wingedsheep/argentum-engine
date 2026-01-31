package com.wingedsheep.mtg.sets.definitions.lorwyn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ExileEffect
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.targeting.TargetCardInGraveyard

/**
 * Rooftop Percher
 * {5}
 * Creature — Shapeshifter
 * 3/3
 * Changeling (This card is every creature type.)
 * Flying
 * When this creature enters, exile up to two target cards from graveyards.
 * You gain 3 life.
 */
val RooftopPercher = card("Rooftop Percher") {
    manaCost = "{5}"
    typeLine = "Creature — Shapeshifter"
    power = 3
    toughness = 3

    keywords(Keyword.CHANGELING, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetCardInGraveyard(count = 2, optional = true)
        effect = CompositeEffect(
            listOf(
                ExileEffect(EffectTarget.ContextTarget(0)),
                GainLifeEffect(3)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Nils Hamm"
    }
}
