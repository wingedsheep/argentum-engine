package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gauntlet of Might
 * {4}
 * Artifact
 * Red creatures get +1/+1.
 * Whenever a Mountain is tapped for mana, its controller adds an additional {R}.
 *
 * The mana half is the High Tide shape as a permanent static: [AdditionalManaOnSourceTap] over
 * every Mountain (no controller predicate), and the extra {R} goes to the player who tapped it.
 * The Mountain subtype is read from projected state, so dual lands with the Mountain type count.
 */
val GauntletOfMight = card("Gauntlet of Might") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "Red creatures get +1/+1.\nWhenever a Mountain is tapped for mana, its controller adds an additional {R}."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.RED))
        )
    }

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN),
            color = Color.RED,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da248001-ed75-4b68-9532-37d3cd5afc4c.jpg?1783948667"
        ruling("2004-10-04", "Dual lands which have Mountain as one of their types produce an extra red mana when tapped for either color.")
    }
}
