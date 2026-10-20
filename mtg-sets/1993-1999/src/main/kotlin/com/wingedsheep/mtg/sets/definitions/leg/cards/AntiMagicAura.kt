package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventEnchantment
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Anti-Magic Aura
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't be the target of spells and can't be enchanted by other Auras.
 *
 * "Can't be the target of spells" is the spells-only flag Lurker uses (abilities may still target
 * it); "can't be enchanted by other Auras" is Consecrate Land's `PreventEnchantment(exceptSource)`,
 * which also dislodges Auras already on the creature through the enchant state-based action.
 */
val AntiMagicAura = card("Anti-Magic Aura") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can't be the target of spells and can't be enchanted by other Auras."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(AbilityFlag.CANT_BE_TARGETED_BY_SPELLS.name, GroupFilter.attachedCreature())
    }
    staticAbility {
        ability = PreventEnchantment(exceptSource = true, filter = GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff78eef1-efaa-4a12-bf5d-fec83c14aff8.jpg?1783948078"
    }
}
