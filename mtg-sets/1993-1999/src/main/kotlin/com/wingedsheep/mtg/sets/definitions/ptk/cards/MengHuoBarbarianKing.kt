package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Meng Huo, Barbarian King
 * {3}{G}{G}
 * Legendary Creature — Human Barbarian Soldier
 * 4/4
 * Other green creatures you control get +1/+1.
 *
 * The lord shape: "other" is the [GroupFilter]'s `excludeSelf`, not a predicate — Meng Huo is green
 * himself and would otherwise pump his own stats.
 */
val MengHuoBarbarianKing = card("Meng Huo, Barbarian King") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Barbarian Soldier"
    power = 4
    toughness = 4
    oracleText = "Other green creatures you control get +1/+1."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.GREEN).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Yang Guangmai"
        flavorText = "The stubborn Meng Huo was captured and released by Kongming seven times before finally surrendering."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e340647c-fb6d-45a6-9f42-235390b40337.jpg"
    }
}
