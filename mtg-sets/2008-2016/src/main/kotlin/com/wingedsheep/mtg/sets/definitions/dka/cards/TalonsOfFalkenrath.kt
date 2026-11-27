package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Talons of Falkenrath
 * {1}{R}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature has "{1}{R}: This creature gets +2/+0 until end of turn."
 *
 * The granted ability's [EffectTarget.Self] is the enchanted creature it was activated on.
 */
val TalonsOfFalkenrath = card("Talons of Falkenrath") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Enchant creature\n" +
        "Enchanted creature has \"{1}{R}: This creature gets +2/+0 until end of turn.\""
    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{1}{R}")
                effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
            }
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8e38239-a9ec-4149-9c90-74dcd46ed95d.jpg"
    }
}
