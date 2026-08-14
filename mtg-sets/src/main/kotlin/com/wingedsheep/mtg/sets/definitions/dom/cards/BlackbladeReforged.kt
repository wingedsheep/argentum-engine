package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Blackblade Reforged
 * {2}
 * Legendary Artifact — Equipment
 * Equipped creature gets +1/+1 for each land you control.
 * Equip legendary creature {3}
 * Equip {7}
 */
val BlackbladeReforged = card("Blackblade Reforged") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each land you control.\n" +
        "Equip legendary creature {3}\n" +
        "Equip {7}"

    // Equipped creature gets +1/+1 for each land you control
    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            toughnessBonus = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land)
        )
    }

    // Equip legendary creature {3} — an "Equip [quality] creature" variant (CR 702.6c).
    equipAbility(
        "{3}",
        quality = "legendary",
        targetFilter = TargetFilter(GameObjectFilter.Creature.legendary().youControl()),
    )

    // Equip {7}
    equipAbility("{7}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Chris Rahn"
        flavorText = "It spilled the blood of one elder dragon. In Gideon's hands, it may yet taste another's."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/862d38d1-e3d0-47e1-a535-ff445b1c55c6.jpg?1562738920"
        ruling("2018-04-27", "\"Equip [quality] creature\" is a variant of the equip keyword meaning \"[Cost]: Attach this Equipment to target [quality] creature you control. Activate only as a sorcery.\"")
    }
}
