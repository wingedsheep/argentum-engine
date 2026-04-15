package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aquitect's Defenses
 * {1}{U}
 * Enchantment — Aura
 * Flash
 * Enchant creature you control
 * When this Aura enters, enchanted creature gains hexproof until end of turn.
 * Enchanted creature gets +1/+2.
 */
val AquitectsDefenses = card("Aquitect's Defenses") {
    manaCost = "{1}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature you control\nWhen this Aura enters, enchanted creature gains hexproof until end of turn. (It can't be the target of spells or abilities your opponents control.)\nEnchanted creature gets +1/+2."

    keywords(Keyword.FLASH)

    auraTarget = Targets.CreatureYouControl

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GrantHexproof(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = ModifyStats(1, 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Ioannis Fiore"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9af9a907-fd2e-4ae0-ac7b-529074b79a14.jpg?1767871750"
    }
}
