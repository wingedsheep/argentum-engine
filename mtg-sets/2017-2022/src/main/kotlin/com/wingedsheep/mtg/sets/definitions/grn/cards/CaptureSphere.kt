package com.wingedsheep.mtg.sets.definitions.grn.cards

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
 * Capture Sphere — Guilds of Ravnica #31 (canonical printing)
 * {3}{U}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * Mystic Restraints' shape: an enter-tap trigger plus the `DOESNT_UNTAP` grant, with flash.
 */
val CaptureSphere = card("Capture Sphere") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
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
        collectorNumber = "31"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a799ac8-5798-4a26-81c1-763d6dcfcbe8.jpg?1783934193"
    }
}
