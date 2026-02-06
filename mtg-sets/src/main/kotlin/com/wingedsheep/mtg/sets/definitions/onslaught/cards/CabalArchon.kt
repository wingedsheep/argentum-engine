package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.targeting.TargetPlayer

/**
 * Cabal Archon
 * {2}{B}
 * Creature — Human Cleric
 * 2/2
 * {B}, Sacrifice a Cleric: Target player loses 2 life and you gain 2 life.
 */
val CabalArchon = card("Cabal Archon") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Cleric"))
        )
        target = TargetPlayer()
        effect = LoseLifeEffect(2, EffectTarget.ContextTarget(0)) then
                GainLifeEffect(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "129"
        artist = "Pete Venters"
        flavorText = "\"You are weak. I am strong. The protocol is obvious.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bdf6e2a-1bf5-4d63-a58b-883cfb1ea0fa.jpg?1562912737"
    }
}
