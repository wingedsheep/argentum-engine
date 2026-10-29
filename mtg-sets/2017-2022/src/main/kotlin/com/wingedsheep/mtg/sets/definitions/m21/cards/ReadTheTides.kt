package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Read the Tides
 * {5}{U}
 * Sorcery
 * Choose one —
 * • Draw three cards.
 * • Return up to two target creatures to their owners' hands.
 */
val ReadTheTides = card("Read the Tides") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Draw three cards.\n" +
        "• Return up to two target creatures to their owners' hands."

    spell {
        modal(chooseCount = 1) {
            mode("Draw three cards") {
                effect = Effects.DrawCards(3)
            }
            mode("Return up to two target creatures to their owners' hands") {
                targets(TargetFilter.Creature, count = 2, optional = true)
                effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "Tommy Arnold"
        flavorText = "Charts and instruments work fine, but the best navigators aren't above a spell or two to nudge the tides in a more favorable direction."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/5360fe8c-41b0-4409-b03e-072f129fb352.jpg?1783930724"
    }
}
