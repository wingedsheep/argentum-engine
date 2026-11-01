package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thrash // Threat (RNA 229) — split-layout spell (CR 709).
 *
 * Thrash {R/G}{R/G} — Instant
 *   Target creature you control deals damage equal to its power to target creature or
 *   planeswalker you don't control.
 *
 * Threat {2}{R}{G} — Sorcery
 *   Create a 4/4 red and green Beast creature token with trample.
 */
val ThrashThreat = card("Thrash // Threat") {
    layout = CardLayout.SPLIT
    colorIdentity = "RG"

    face("Thrash") {
        manaCost = "{R/G}{R/G}"
        typeLine = "Instant"
        oracleText = "Target creature you control deals damage equal to its power to target creature or planeswalker you don't control."

        spell {
            val yours = target(TargetFilter.Creature.youControl())
            val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
            effect = Effects.DealDamage(DynamicAmounts.powerOf(yours), victim, damageSource = yours)
        }
    }

    face("Threat") {
        manaCost = "{2}{R}{G}"
        typeLine = "Sorcery"
        oracleText = "Create a 4/4 red and green Beast creature token with trample."

        spell {
            effect = Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.RED, Color.GREEN),
                creatureTypes = setOf("Beast"),
                keywords = setOf(Keyword.TRAMPLE),
                imageUri = "https://cards.scryfall.io/normal/front/8/7/87ec9ade-1e06-43c9-97df-55465cabdf32.jpg?1783933602"
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2eefd8c1-96ce-4d7a-8aaf-29c35d634dda.jpg?1783933625"
        ruling(
            "2019-01-25",
            "If either target is an illegal target as Thrash tries to resolve, the creature you control won't deal damage."
        )
    }
}
