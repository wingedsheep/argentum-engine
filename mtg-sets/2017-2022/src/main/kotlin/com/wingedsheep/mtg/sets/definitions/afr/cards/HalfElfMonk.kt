package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Half-Elf Monk
 * {3}{W}
 * Creature — Human Elf Monk
 * 1/4
 * Vigilance
 * Stunning Strike — {1}{W}, {T}: Tap target creature.
 */
val HalfElfMonk = card("Half-Elf Monk") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Elf Monk"
    oracleText = "Vigilance\nStunning Strike — {1}{W}, {T}: Tap target creature."
    power = 1
    toughness = 4
    keywords(Keyword.VIGILANCE)
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Zoltan Boros"
        flavorText = "\"This fight was over the moment you picked up that clumsy cleaver of a sword.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f10f1687-fb20-4ad2-bad7-52e4470b35e4.jpg?1783926531"
    }
}
