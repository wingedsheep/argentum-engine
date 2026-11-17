package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreatureStat
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Grim Contest
 * {1}{B}{G}
 * Instant
 * Choose target creature you control and target creature an opponent controls. Each of those
 * creatures deals damage equal to its toughness to the other.
 *
 * Not a fight: both creatures deal their toughness at once, with no fought event.
 */
val GrimContest = card("Grim Contest") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Choose target creature you control and target creature an opponent controls. " +
        "Each of those creatures deals damage equal to its toughness to the other."

    spell {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        effect = Effects.MutualDamage(mine, theirs, CreatureStat.TOUGHNESS)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Phill Simmer"
        flavorText = "The invader hoped he could survive the beast's jaws and emerge through its rotting skin."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24c44722-c65a-41ae-92a9-9c9d7d759a8e.jpg?1783938673"
        ruling("2014-11-24", "If either target is an illegal target as Grim Contest tries to resolve, neither creature will deal or be dealt damage.")
    }
}
