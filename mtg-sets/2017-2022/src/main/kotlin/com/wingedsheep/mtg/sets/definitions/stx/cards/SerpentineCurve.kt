package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serpentine Curve — Strixhaven: School of Mages #52 (canonical printing)
 * {3}{U} · Sorcery
 *
 * Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it, where X is one
 * plus the total number of instant and sorcery cards you own in exile and in your graveyard.
 *
 * Body of Research's Fractal-then-counters shape with Slime Against Humanity's two-zone tally:
 * face-up instant and sorcery cards you own in exile plus those in your graveyard, plus one.
 * Serpentine Curve is still on the stack while X is read, so it never counts itself, and
 * face-down exiled cards don't count (rulings).
 */
val SerpentineCurve = card("Serpentine Curve") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it, " +
        "where X is one plus the total number of instant and sorcery cards you own in exile and in " +
        "your graveyard."

    spell {
        val spells = GameObjectFilter.InstantOrSorcery.faceUp()
        val counterCount = 1 + (DynamicAmounts.zone(Player.You, Zone.EXILE, spells).count() +
            DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, spells).count())

        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN, Color.BLUE),
            creatureTypes = setOf(Subtype.FRACTAL.value)
        ) then Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            counterCount,
            EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Kekai Kotaki"
        flavorText = "\"Math bites.\"\n—Bim, Lorehold mage-student"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d651b9e9-d723-4340-a010-d71b2a697e73.jpg?1783927375"
        ruling("2021-04-16", "Serpentine Curve itself is not yet in your graveyard when you determine the value of X.")
        ruling("2021-04-16", "If you own face-down cards in exile, they won't count toward the value of X, even if you're allowed to look at them and you know they are instant or sorcery cards.")
    }
}
