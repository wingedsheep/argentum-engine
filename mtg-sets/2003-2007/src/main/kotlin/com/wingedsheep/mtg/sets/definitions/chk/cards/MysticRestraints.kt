package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mystic Restraints
 * {2}{U}{U}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * Tangle Kelp's enter-tap trigger (`drk/cards/TangleKelp.kt`) plus Shackles' unconditional
 * `DOESNT_UNTAP` grant, with flash.
 */
val MysticRestraints = card("Mystic Restraints") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    keywords(Keyword.FLASH)
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, tap enchanted creature."
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96cc4428-3f86-4359-927d-4009ada52c5d.jpg?1783944325"
    }
}
