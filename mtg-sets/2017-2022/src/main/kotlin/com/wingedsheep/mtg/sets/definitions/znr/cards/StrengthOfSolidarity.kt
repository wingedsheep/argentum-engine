package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Strength of Solidarity — Zendikar Rising #206
 * {G} · Sorcery
 *
 * Choose target creature you control. Put a +1/+1 counter on it for each creature in your party.
 *
 * The party size is read as the spell resolves.
 */
val StrengthOfSolidarity = card("Strength of Solidarity") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose target creature you control. Put a +1/+1 counter on it for each creature in your " +
        "party. (Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.partySize(), t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Svetlin Velinov"
        flavorText = "\"This expedition doesn't succeed unless we all succeed!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/090b3da4-c942-480e-9997-bb9c20e8d62d.jpg?1783929330"
    }
}
