package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wall of Tombstones — Legends #129
 * {1}{B} · Creature — Wall · 0/1
 *
 * Defender
 * At the beginning of your upkeep, this creature's base toughness becomes equal to 1 plus the
 * number of creature cards in your graveyard. (This effect lasts indefinitely.)
 *
 * Sentinel's shape on an upkeep trigger: a layer-7b set-base-toughness on itself, the count frozen
 * at resolution (`reevaluateContinuously = false`) with `Duration.Permanent`. Each upkeep's new
 * setting wins over the previous one on timestamp.
 */
val WallOfTombstones = card("Wall of Tombstones") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 1
    oracleText = "Defender (This creature can't attack.)\nAt the beginning of your upkeep, this creature's " +
        "base toughness becomes equal to 1 plus the number of creature cards in your graveyard. " +
        "(This effect lasts indefinitely.)"

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.SetBaseToughness(
            target = EffectTarget.Self,
            toughness = DynamicAmounts.creatureCardsInYourGraveyard() + 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55da1e86-fe18-486a-b510-f941e6f6e378.jpg?1783948060"
    }
}
