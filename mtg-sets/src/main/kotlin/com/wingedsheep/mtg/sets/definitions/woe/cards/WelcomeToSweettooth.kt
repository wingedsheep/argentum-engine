package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Welcome to Sweettooth
 * {1}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 1/1 white Human creature token.
 * II — Create a Food token.
 * III — Put X +1/+1 counters on target creature you control, where X is one plus the number of
 *   Foods you control.
 *
 * Chapters I and II are the shared WOE Human token ([woeHumanToken]) and the predefined Food token.
 *
 * Chapter III's X is "one plus the number of Foods you control": a [DynamicAmount.Add] over a
 * battlefield count of Food *artifacts* — not just Food tokens, per the Scryfall ruling, which is
 * exactly what the `Artifact.withSubtype("Food")` filter matches. X is evaluated as the chapter
 * ability resolves, and the Food token minted by chapter II two turns earlier is still counted if
 * it's around. The chapter targets, so the whole ability does nothing if the chosen creature has
 * left or become an illegal target by resolution.
 */
val WelcomeToSweettooth = card("Welcome to Sweettooth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 1/1 white Human creature token.\n" +
        "II — Create a Food token.\n" +
        "III — Put X +1/+1 counters on target creature you control, where X is one plus the number " +
        "of Foods you control."

    sagaChapter(1) {
        effect = woeHumanToken()
    }

    sagaChapter(2) {
        effect = Effects.CreateFood()
    }

    sagaChapter(3) {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddDynamicCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            DynamicAmount.Add(
                DynamicAmount.Fixed(1),
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Artifact.withSubtype("Food"),
                ).count(),
            ),
            creature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e629a31-2e06-4f95-9628-34670dcf68b9.jpg?1783915075"

        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a creature type."
        )
    }
}
