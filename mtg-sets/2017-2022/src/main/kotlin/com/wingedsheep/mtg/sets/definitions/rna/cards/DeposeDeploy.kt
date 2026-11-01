package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Depose // Deploy (RNA 225) — split-layout spell (CR 709).
 *
 * Depose {1}{W/U} — Instant
 *   Tap target creature.
 *   Draw a card.
 *
 * Deploy {2}{W}{U} — Instant
 *   Create two 1/1 colorless Thopter artifact creature tokens with flying, then you gain 1 life
 *   for each creature you control.
 *
 * Deploy's life count is read after the Thopters enter, so they count toward it.
 */
val DeposeDeploy = card("Depose // Deploy") {
    layout = CardLayout.SPLIT
    colorIdentity = "WU"

    face("Depose") {
        manaCost = "{1}{W/U}"
        typeLine = "Instant"
        oracleText = "Tap target creature.\nDraw a card."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Tap(creature) then Effects.DrawCards(1)
        }
    }

    face("Deploy") {
        manaCost = "{2}{W}{U}"
        typeLine = "Instant"
        oracleText = "Create two 1/1 colorless Thopter artifact creature tokens with flying, " +
            "then you gain 1 life for each creature you control."

        spell {
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Thopter"),
                keywords = setOf(Keyword.FLYING),
                count = 2,
                artifactToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/8/d/8dd768c3-f278-41f8-b329-8403391d6326.jpg?1783933601",
            ) then Effects.GainLife(DynamicAmounts.creaturesYouControl())
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Sara Winters"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab0ba4ef-9e82-4177-a80f-8fa6f6a5bd60.jpg?1783933627"
        ruling(
            "2019-01-25",
            "You create Thopters and gain life all while Deploy is resolving. Nothing can happen between the two, " +
                "and no player may choose to take actions."
        )
        ruling(
            "2019-01-25",
            "If the target creature is an illegal target by the time Depose tries to resolve, the spell doesn't resolve. " +
                "You don't draw a card. If the target is legal but not tapped (most likely because it was already tapped), " +
                "you do draw a card."
        )
    }
}
