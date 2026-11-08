package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chandra's Revolution
 * {3}{R}
 * Sorcery
 * Chandra's Revolution deals 4 damage to target creature. Tap target land. That land doesn't
 * untap during its controller's next untap step.
 */
val ChandrasRevolution = card("Chandra's Revolution") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Chandra's Revolution deals 4 damage to target creature. Tap target land. That land doesn't untap during its controller's next untap step."

    spell {
        val creature = target(TargetFilter.Creature)
        val land = target(TargetFilter.Land)
        effect = Effects.DealDamage(4, creature) then
            Effects.Tap(land) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, land, Duration.UntilAfterAffectedControllersNextUntap())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Clint Cearley"
        flavorText = "Chandra was driven by her anger, directed particularly at the man who had taken her father from her."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fcee99c-72a0-4db1-b4c9-65c878284450.jpg?1783936756"
        ruling("2017-02-09", "Chandra's Revolution can target a land that's already tapped. That land won't untap during its controller's next untap step.")
    }
}
