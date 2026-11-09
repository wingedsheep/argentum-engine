package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Hope Against Hope
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 for each creature you control.
 * As long as enchanted creature is a Human, it has first strike.
 *
 * The bonus counts creatures the Aura's controller controls (the Blanchwood Armor shape); the
 * first-strike grant is gated on the enchanted creature's projected subtype (the Bonds of Faith shape).
 */
val HopeAgainstHope = card("Hope Against Hope") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 for each creature you control.\n" +
        "As long as enchanted creature is a Human, it has first strike."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count(),
            toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count()
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.attachedCreature())
        condition = EnchantedCreatureHasSubtype(Subtype.HUMAN)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "David Seguin"
        flavorText = "\"I have given my life to the church, my soul to Avacyn, and my heart to the people.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6f7c6f5-159a-40ed-823a-8740a1ce373e.jpg?1783937817"
        ruling(
            "2016-04-08",
            "Hope Against Hope's second ability counts the enchanted creature, so that creature will " +
                "normally get at least +1/+1."
        )
    }
}
