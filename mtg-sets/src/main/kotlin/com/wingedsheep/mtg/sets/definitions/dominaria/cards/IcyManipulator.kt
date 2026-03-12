package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Icy Manipulator
 * {4}
 * Artifact
 * {1}, {T}: Tap target artifact, creature, or land.
 */
val IcyManipulator = card("Icy Manipulator") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Tap target artifact, creature, or land."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val permanent = target(
            "artifact, creature, or land",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land
                )
            )
        )
        effect = Effects.Tap(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Titus Lunter"
        flavorText = "\"Ice may thaw, but malice never does.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fd9b205-817e-4ca4-8a29-3c2b6cbf7207.jpg?1562740411"
    }
}
