package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.FreeFirstEquipEachTurn
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.AttachmentKind
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Bruenor Battlehammer
 * {2}{R}{W}
 * Legendary Creature — Dwarf Warrior
 * 5/3
 *
 * Each creature you control gets +2/+0 for each Equipment attached to it.
 * You may pay {0} rather than pay the equip cost of the first equip ability you activate each turn.
 *
 * The anthem is re-evaluated per affected creature: [EffectTarget.AffectedEntity] is the creature
 * the bonus lands on, so each one counts only *its own* Equipment (projected subtypes). The second
 * ability is the controller-scoped [FreeFirstEquipEachTurn] Kíli the Resourceful uses.
 */
val BruenorBattlehammer = card("Bruenor Battlehammer") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Dwarf Warrior"
    oracleText = "Each creature you control gets +2/+0 for each Equipment attached to it.\n" +
        "You may pay {0} rather than pay the equip cost of the first equip ability you activate each turn."
    power = 5
    toughness = 3

    staticAbility {
        val equipmentOnIt = DynamicAmounts.propertyOf(
            EffectTarget.AffectedEntity,
            EntityNumericProperty.AttachmentCount(AttachmentKind.EQUIPMENT)
        )
        ability = GrantDynamicStats(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            powerBonus = equipmentOnIt * 2,
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    staticAbility {
        ability = FreeFirstEquipEachTurn
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Wayne Reynolds"
        flavorText = "\"Knew I'd find ye in trouble if I came out an' looked for ye!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53e1eb00-412e-4c25-bc7b-6d074330fd97.jpg?1783926450"
    }
}
