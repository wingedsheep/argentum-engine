package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thundermaw Hellkite
 * {3}{R}{R}
 * Creature — Dragon
 * 5/5
 * Flying
 * Haste
 * When this creature enters, it deals 1 damage to each creature with flying your opponents
 * control. Tap those creatures.
 *
 * One snapshot of the group drives both halves, so every creature that was dealt the damage —
 * or would have been, had it not been prevented or redirected (2017-11-17 ruling) — is tapped.
 */
val ThundermawHellkite = card("Thundermaw Hellkite") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\nHaste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "When this creature enters, it deals 1 damage to each creature with flying your opponents control. " +
        "Tap those creatures."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).opponentControls()),
            Effects.DealDamage(1, EffectTarget.IterationEntity) then
                Effects.Tap(EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "150"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0476e0f-61df-46a6-aaf1-8ee79c701160.jpg?1783940479"
        ruling(
            "2017-11-17",
            "Thundermaw Hellkite will deal 1 damage to each creature with flying your opponents control " +
                "whether those creatures are tapped or untapped."
        )
        ruling(
            "2017-11-17",
            "If the damage that would be dealt to a creature with flying is prevented, that creature will " +
                "still be tapped. If that damage is redirected to another creature, the creature with flying " +
                "will be tapped, not necessarily the creature that ultimately was dealt damage."
        )
    }
}
