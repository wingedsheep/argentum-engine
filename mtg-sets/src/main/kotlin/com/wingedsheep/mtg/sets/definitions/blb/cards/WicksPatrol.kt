package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Wick's Patrol
 * {4}{B}{B}
 * Creature — Rat Warlock
 * 5/3
 *
 * When this creature enters, mill three cards. When you do, target creature
 * an opponent controls gets -X/-X until end of turn, where X is the greatest
 * mana value among cards in your graveyard.
 */
val WicksPatrol = card("Wick's Patrol") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Warlock"
    oracleText = "When this creature enters, mill three cards. When you do, target creature an opponent controls gets -X/-X until end of turn, where X is the greatest mana value among cards in your graveyard."
    power = 5
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val greatestMV = DynamicAmounts.zone(Player.You, Zone.GRAVEYARD).maxManaValue()
        val negX = DynamicAmount.Multiply(greatestMV, -1)
        effect = ReflexiveTriggerEffect(
            action = LibraryPatterns.mill(3),
            optional = false,
            reflexiveEffect = Effects.ModifyStats(negX, negX, EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(Targets.CreatureOpponentControls)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Dmitry Burmak"
        flavorText = "\"What a hideous creature! And the worm's no looker either.\" —Grumble, rival marshstalker"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fa0c53d-fe7b-4b8b-ad81-7967ca318ff7.jpg?1721426557"
    }
}
