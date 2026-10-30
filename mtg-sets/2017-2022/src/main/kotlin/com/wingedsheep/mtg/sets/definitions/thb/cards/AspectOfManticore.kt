package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aspect of Manticore
 * {2}{R}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * When this Aura enters, enchanted creature gains first strike until end of turn.
 * Enchanted creature gets +2/+0.
 *
 * Pemmin's Aura's `GrantKeyword(…, EnchantedCreature)` as an enters trigger. "Enchanted creature"
 * resolves to the host as it last existed when the Aura has already left (CR 608.2h), so the
 * creature it last enchanted still gains first strike (ruling).
 */
val AspectOfManticore = card("Aspect of Manticore") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, enchanted creature gains first strike until end of turn.\n" +
        "Enchanted creature gets +2/+0."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.EnchantedCreature)
        description = "When this Aura enters, enchanted creature gains first strike until end of turn."
    }

    staticAbility {
        ability = ModifyStats(+2, 0)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f67d0091-1925-44a5-89f3-21c2afd5665c.jpg?1783931556"
        ruling(
            "2020-01-24",
            "If Aspect of Manticore leaves the battlefield before its enters-the-battlefield triggered ability " +
                "resolves, the creature it last enchanted before it left gains first strike until end of turn."
        )
    }
}
