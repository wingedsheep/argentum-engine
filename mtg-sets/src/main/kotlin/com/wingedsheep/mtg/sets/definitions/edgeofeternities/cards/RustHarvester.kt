package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Rust Harvester
 * {R}
 * Artifact Creature — Robot
 * Menace
 * {2}, {T}, Exile an artifact card from your graveyard: Put a +1/+1 counter on this creature, then it deals damage equal to its power to any target.
 */
val RustHarvester = card("Rust Harvester") {
    manaCost = "{R}"
    typeLine = "Artifact Creature — Robot"
    oracleText = "Menace\n{2}, {T}, Exile an artifact card from your graveyard: Put a +1/+1 counter on this creature, then it deals damage equal to its power to any target."
    power = 1
    toughness = 1

    // Menace keyword
    keywords(Keyword.MENACE)

    // Activated ability: {2}, {T}, Exile an artifact card from your graveyard: Put a +1/+1 counter on this creature, then it deals damage equal to its power to any target
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.ExileFromGraveyard(1, GameObjectFilter.Artifact)
        )
        
        // Put a +1/+1 counter on this creature
        val addCounterEffect = Effects.AddCounters("+1/+1", 1, com.wingedsheep.sdk.scripting.targets.EffectTarget.Self)
        
        // Then it deals damage equal to its power to any target
        val damageTarget = target("any target", Targets.Any)
        val damageEffect = Effects.DealDamage(
            amount = com.wingedsheep.sdk.dsl.DynamicAmounts.sourcePower(),
            target = damageTarget
        )
        
        effect = Effects.Composite(addCounterEffect, damageEffect)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Jake Murray"
        flavorText = "It scrounges Kavaron's wasteland, unaware that its home can no longer support its purpose."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7ce58765-d2f4-4fbf-8635-580c9400ff2e.jpg?1752947195"
    }
}
