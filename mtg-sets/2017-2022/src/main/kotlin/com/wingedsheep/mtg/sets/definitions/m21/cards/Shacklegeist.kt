package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shacklegeist — Core Set 2021 #70 (canonical printing)
 * {1}{U} · Creature — Spirit · 2/2
 *
 * Flying
 * This creature can block only creatures with flying.
 * Tap two untapped Spirits you control: Tap target creature you don't control.
 *
 * Aphetto Grifter's cost shape: "Tap two untapped Spirits you control" has no {T} symbol, so
 * Shacklegeist may be one of the two Spirits and summoning sickness doesn't stop it.
 */
val Shacklegeist = card("Shacklegeist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "This creature can block only creatures with flying.\n" +
        "Tap two untapped Spirits you control: Tap target creature you don't control."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }

    activatedAbility {
        cost = Costs.TapPermanents(2, GameObjectFilter.Permanent.withSubtype("Spirit"))
        val creature = target(TargetFilter.Creature.opponentControls())
        effect = Effects.Tap(creature)
        description = "Tap two untapped Spirits you control: Tap target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Igor Kieryluk"
        flavorText = "The Blessed Sleep doesn't come easily to a mind chained by regrets."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a5a88e3-e73c-4b34-a645-06fe27e68cee.jpg?1783930719"
    }
}
