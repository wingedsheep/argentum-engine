package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val JadeMonolith = card("Jade Monolith") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{1}: The next time a source of your choice would deal damage to target creature this turn, that source deals that damage to you instead."
    activatedAbility {
        cost = Costs.Mana("{1}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.RedirectDamageFromChosenSource(
            protectedTarget = creature, redirectTo = EffectTarget.Controller
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "252"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a77e0f1-449d-4a7d-9fa0-ba7598f7a73a.jpg?1783948665"
    }
}
