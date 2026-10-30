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
 * Starlit Mantle
 * {1}{U}
 * Enchantment — Aura
 * Flash
 * Enchant creature you control
 * When this Aura enters, enchanted creature gains hexproof until end of turn.
 * Enchanted creature gets +1/+1.
 *
 * Cradle of Safety's text. `EffectTarget.EnchantedCreature` falls back to the Aura's last-known
 * host, so if the Aura leaves before its trigger resolves, the creature it last enchanted still
 * gains hexproof (ruling).
 */
val StarlitMantle = card("Starlit Mantle") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Enchant creature you control\n" +
        "When this Aura enters, enchanted creature gains hexproof until end of turn. " +
        "(It can't be the target of spells or abilities your opponents control.)\n" +
        "Enchanted creature gets +1/+1."

    keywords(Keyword.FLASH)
    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.EnchantedCreature)
        description = "When this Aura enters, enchanted creature gains hexproof until end of turn."
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68c53625-6928-494a-af87-7eee2f6643a5.jpg?1783931578"
        ruling(
            "2020-01-24",
            "If Starlit Mantle leaves the battlefield before its enters-the-battlefield triggered ability " +
                "resolves, the creature it last enchanted before it left gains hexproof until end of turn."
        )
    }
}
