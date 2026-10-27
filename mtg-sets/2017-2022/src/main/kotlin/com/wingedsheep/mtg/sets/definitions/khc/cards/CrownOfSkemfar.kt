package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crown of Skemfar
 * {2}{G}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 for each Elf you control and has reach.
 * {2}{G}: Return this card from your graveyard to your hand.
 */
val CrownOfSkemfar = card("Crown of Skemfar") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 for each Elf you control and has reach.\n" +
        "{2}{G}: Return this card from your graveyard to your hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // A bare tribal noun ("each Elf") counts every Elf permanent you control, not only creatures.
    staticAbility {
        val elves = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Permanent.withSubtype(Subtype.ELF)
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = elves,
            toughnessBonus = elves
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        description = "{2}{G}: Return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Jason Felix"
        flavorText = "Lathril, queen of the ancient Einir, left a powerful legacy for her mortal descendants."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6297586-8953-408e-a38c-2239d45807e1.jpg?1783928337"
        ruling("2021-02-05", "The power/toughness bonus given by Crown of Skemfar will change as the number of Elves you control changes.")
    }
}
