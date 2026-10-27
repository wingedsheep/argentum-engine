package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Blinding Beam
 * {2}{W}
 * Instant
 * Choose one —
 * • Tap two target creatures.
 * • Creatures don't untap during target player's next untap step.
 * Entwine {1} (Choose both if you pay the entwine cost.)
 */
val BlindingBeam = card("Blinding Beam") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Tap two target creatures.\n" +
        "• Creatures don't untap during target player's next untap step.\n" +
        "Entwine {1} (Choose both if you pay the entwine cost.)"

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            additionalManaCostPerExtraMode = "{1}"
        ) {
            mode("Tap two target creatures") {
                target = TargetObject(filter = TargetFilter.Creature, count = 2)
                effect = Effects.ForEachTarget(Effects.Tap(EffectTarget.ContextTarget(0)))
            }
            mode("Creatures don't untap during target player's next untap step") {
                val player = target(Targets.Player)
                effect = Effects.SkipUntap(
                    target = player,
                    permanents = GameObjectFilter.Creature
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Doug Chaffee"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b73f6e4c-9da7-45ec-b786-1b2f59d6b73b.jpg?1783944562"
        ruling(
            "2013-06-07",
            "The second mode affects all creatures during the player’s next untap step, including " +
                "creatures controlled by other players and creatures that weren’t on the battlefield " +
                "when Blinding Beam resolved."
        )
    }
}
