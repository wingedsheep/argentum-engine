package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RegenerateEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Boneknitter
 * {1}{B}
 * Creature — Zombie Cleric
 * 1/1
 * {1}{B}: Regenerate target Zombie.
 * Morph {2}{B} (You may cast this card face down as a 2/2 creature for {3}.
 * Turn it face up any time for its morph cost.)
 */
val Boneknitter = card("Boneknitter") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Zombie Cleric"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        target = TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Zombie"))
        )
        effect = RegenerateEffect(EffectTarget.ContextTarget(0))
    }

    morph = "{2}{B}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Thomas M. Baxa"
        flavorText = "It mends the not-so-dearly departed."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3b5e656-6a9d-48a8-a088-2648c4e73e19.jpg?1562940379"
    }
}
