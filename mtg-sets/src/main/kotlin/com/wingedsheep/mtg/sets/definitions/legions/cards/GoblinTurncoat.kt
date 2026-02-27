package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Turncoat
 * {1}{B}
 * Creature — Goblin Mercenary
 * 2/1
 * Sacrifice a Goblin: Regenerate Goblin Turncoat.
 */
val GoblinTurncoat = card("Goblin Turncoat") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Goblin Mercenary"
    power = 2
    toughness = 1
    oracleText = "Sacrifice a Goblin: Regenerate Goblin Turncoat."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
        effect = RegenerateEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Jim Nelson"
        flavorText = "Goblins won't betray their own kind for any price. They'll do it for a very specific price."
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ac74e64-8831-4af2-9c6d-22c533389144.jpg?1562903706"
    }
}
