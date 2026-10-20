package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Seeker
 * {2}{W}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't be blocked except by artifact creatures and/or white creatures.
 */
val Seeker = card("Seeker") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can't be blocked except by artifact creatures and/or white creatures."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantBeBlockedExceptBy(
            blockerFilter = GameObjectFilter.ArtifactCreature or
                GameObjectFilter.Creature.withColor(Color.WHITE),
            filter = GroupFilter.attachedCreature()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df608b59-cc07-4e1d-b6d6-f15e69b15b92.jpg?1783948080"
    }
}
