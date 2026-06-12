package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Zero Point Ballad
 * {X}{B}
 * Sorcery
 * Destroy all creatures with toughness X or less. You lose X life. If X is 6 or more,
 * return a creature card put into a graveyard this way to the battlefield under your control.
 */
val ZeroPointBallad = card("Zero Point Ballad") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures with toughness X or less. You lose X life. " +
        "If X is 6 or more, return a creature card put into a graveyard this way " +
        "to the battlefield under your control."

    spell {
        effect = Effects.Composite(
            listOf(
                Effects.DestroyAll(
                    filter = GameObjectFilter.Creature.toughnessAtMostX(),
                    storeDestroyedAs = "destroyed"
                ),
                Effects.LoseLife(DynamicAmount.XValue, EffectTarget.Controller),
                ConditionalEffect(
                    condition = Compare(
                        DynamicAmount.XValue,
                        ComparisonOperator.GTE,
                        DynamicAmount.Fixed(6)
                    ),
                    effect = Effects.Pipeline {
                        val reanimated = chooseExactly(
                            1, from = CollectionSlot("destroyed"),
                            chooser = Chooser.Controller,
                            // Oracle reads "a creature card" — tokens cease to exist on death.
                            filter = GameObjectFilter.Creature.nontoken(),
                            name = "reanimated"
                        )
                        move(reanimated, destination = CardDestination.ToZone(Zone.BATTLEFIELD))
                    }
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "David Astruga"
        flavorText = "\"Beyond the supervoid, existence collapses into a singular point. " +
            "We are all dead. We all live.\"\n—Xu-Ifit, osteoharmonist"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59cf9f4d-54cd-4cda-9726-65e16100ab46.jpg?1752947069"
    }
}
