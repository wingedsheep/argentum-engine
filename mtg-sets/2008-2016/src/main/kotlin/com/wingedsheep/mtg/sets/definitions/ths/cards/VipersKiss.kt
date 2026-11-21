package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Viper's Kiss
 * {B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -1/-1, and its activated abilities can't be activated.
 *
 * Sicken's -1/-1 static plus Arrest's activation lock (mana abilities included).
 */
val VipersKiss = card("Viper's Kiss") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -1/-1, and its activated abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-1, -1)
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Svetlin Velinov"
        flavorText = "\"I could heal a snakebite. Looks like you got bitten by a constellation.\"\n" +
            "—Aescalos, healer of Pharika"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0906ebfc-4756-4caf-83ff-859e181f3bef.jpg?1783939769"
    }
}
