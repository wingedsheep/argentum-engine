package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Suppression Bonds
 * {3}{W}
 * Enchantment — Aura
 *
 * Enchant nonland permanent
 * Enchanted permanent can't attack or block, and its activated abilities can't be activated.
 *
 * Same lock as Detention Vortex without the opponent-only destroy ability.
 */
val SuppressionBonds = card("Suppression Bonds") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant nonland permanent\n" +
        "Enchanted permanent can't attack or block, and its activated abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter.NonlandPermanent)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Chris Rallis"
        flavorText = "\"A master of hieromancy can see the advantage in any scenario.\"\n—Warden Hixus, to Kytheon"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d34c717b-224b-4289-826d-f5b1f7c3dbfb.jpg?1783938357"
    }
}
