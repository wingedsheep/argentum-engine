package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Animate Wall
 * {W}
 * Enchantment — Aura
 * Enchant Wall
 * Enchanted Wall can attack as though it didn't have defender.
 *
 * The grant is a battlefield-scoped [CanAttackDespiteDefender] whose filter is source-relative
 * (`attachedToBySource`): it covers exactly the permanent this Aura is attached to, re-read each
 * time attackers are declared.
 */
val AnimateWall = card("Animate Wall") {
    manaCost = "{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant Wall\nEnchanted Wall can attack as though it didn't have defender."
    colorIdentity = "W"
    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype("Wall")))

    staticAbility {
        ability = CanAttackDespiteDefender(
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Wall").attachedToBySource())
        )
    }

    metadata {
        ruling("2007-09-16", "This is a change from the most recent wording. As was the case in the past, Animate Wall can now enchant only a Wall.")
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5c83259-9b90-47c2-b48e-c7d78519e792.jpg?1783948717"
    }
}
