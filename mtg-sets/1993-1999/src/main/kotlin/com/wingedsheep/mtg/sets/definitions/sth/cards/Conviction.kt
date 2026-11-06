package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Conviction
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+3.
 * {W}: Return this Aura to its owner's hand.
 */
val Conviction = card("Conviction") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+3.\n" +
        "{W}: Return this Aura to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 3)
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Paolo Parente"
        flavorText = "It was not the minotaur's shoulders but his soul that bore the heaviest weight."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/190332b3-6a1c-4c25-af61-d8923fc9a0c3.jpg"
        ruling("2018-12-07", "Players don't have priority to cast spells and activate abilities between combat damage being assigned and being dealt. This means that if you want to return Conviction to its owner's hand before combat damage is dealt, you must do so before combat damage is assigned (and the creature will no longer get +1/+3).")
        ruling("2018-12-07", "Because damage remains marked on a creature until it's removed as the turn ends, nonlethal damage dealt to the enchanted creature may become lethal if you return Conviction to its owner's hand during that turn.")
    }
}
