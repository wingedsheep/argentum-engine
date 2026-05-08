package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sporecrown Thallid
 * {1}{G}
 * Creature — Fungus
 * 2/2
 * Each other creature you control that's a Fungus or Saproling gets +1/+1.
 */
val SporecrownThallid = card("Sporecrown Thallid") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Fungus"
    power = 2
    toughness = 2
    oracleText = "Each other creature you control that's a Fungus or Saproling gets +1/+1."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.youControl().withSubtype("Fungus") or
                    GameObjectFilter.Creature.youControl().withSubtype("Saproling"),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Bram Sels"
        flavorText = "\"The identifying ornamental growths of alpha thallids may be hereditary, or catalyzed by some chemical signal.\" —Sarpadian Empires, vol. III"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/658b93b5-e16a-4a1a-bd26-74a9f25caffe.jpg?1562736855"
    }
}
