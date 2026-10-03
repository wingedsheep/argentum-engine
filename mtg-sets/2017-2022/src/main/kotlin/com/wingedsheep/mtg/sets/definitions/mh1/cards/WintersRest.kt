package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Winter's Rest
 * {1}{U}
 * Snow Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * As long as you control another snow permanent, enchanted creature doesn't untap during its
 * controller's untap step.
 */
val WintersRest = card("Winter's Rest") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Snow Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "As long as you control another snow permanent, enchanted creature doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, tap enchanted creature."
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
        condition = Conditions.YouControl(GameObjectFilter.Permanent.snow(), excludeSelf = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fdaf6b6-aca7-49e5-a4da-bf8c08b4a055.jpg?1783933134"

        ruling("2019-06-14", "If you lose control of all of your other snow permanents, then control a new one, the effect of Winter's Rest will apply to the enchanted creature again.")
    }
}
