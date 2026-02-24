package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Nosy Goblin
 * {2}{R}
 * Creature — Goblin
 * 2/1
 * {T}, Sacrifice Nosy Goblin: Destroy target face-down creature.
 */
val NosyGoblin = card("Nosy Goblin") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1
    oracleText = "{T}, Sacrifice Nosy Goblin: Destroy target face-down creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.faceDown())
        ))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "220"
        artist = "Thomas M. Baxa"
        flavorText = "To his surprise, Furt discovered that the strange creatures were not at all like bugs."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70ea023e-e66d-4049-b7bc-5e660804f088.jpg?1562921618"
    }
}
