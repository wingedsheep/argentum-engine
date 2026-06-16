package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Tawnos's Wand
 * {4}
 * Artifact
 * {2}, {T}: Target creature with power 2 or less can't be blocked this turn.
 */
val TawnossWand = card("Tawnos's Wand") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Target creature with power 2 or less can't be blocked this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val creature = target(
            "target creature with power 2 or less",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.powerAtMost(2)))
        )
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        description = "{2}, {T}: Target creature with power 2 or less can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/978f09dd-121a-4da5-ba16-5c03fbdce084.jpg?1562927106"
    }
}
