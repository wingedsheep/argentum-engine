package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EquipmentAttachRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Konda's Banner
 * {2}
 * Legendary Artifact — Equipment
 * Konda's Banner can be attached only to a legendary creature.
 * Creatures that share a color with equipped creature get +1/+1.
 * Creatures that share a creature type with equipped creature get +1/+1.
 * Equip {2}
 *
 * The two anthems are separate statics, so a creature sharing both a color and a type gets +2/+2
 * and sharing several of either still gets only +1/+1 from that half (the card's ruling). The
 * equipped creature shares with itself. Unattached, neither filter has a reference and nothing
 * gets a bonus.
 */
val KondasBanner = card("Konda's Banner") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Konda's Banner can be attached only to a legendary creature.\n" +
        "Creatures that share a color with equipped creature get +1/+1.\n" +
        "Creatures that share a creature type with equipped creature get +1/+1.\n" +
        "Equip {2}"

    staticAbility {
        ability = EquipmentAttachRestriction(GameObjectFilter.Creature.legendary())
    }

    staticAbility {
        ability = ModifyStats(
            1, 1,
            GroupFilter(GameObjectFilter.Creature.sharingColorWith(EffectTarget.EquippedCreature))
        )
    }

    staticAbility {
        ability = ModifyStats(
            1, 1,
            GroupFilter(GameObjectFilter.Creature.sharingCreatureTypeWith(EffectTarget.EquippedCreature))
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/759b2f39-e1b1-44aa-b49a-00ef6926a6ac.jpg?1783944278"
    }
}
