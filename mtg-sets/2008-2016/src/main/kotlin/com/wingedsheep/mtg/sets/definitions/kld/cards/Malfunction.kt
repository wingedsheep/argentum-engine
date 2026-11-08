package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Malfunction — Kaladesh #55
 * {3}{U} · Enchantment — Aura
 *
 * Enchant artifact or creature
 * When this Aura enters, tap enchanted permanent.
 * Enchanted permanent doesn't untap during its controller's untap step.
 *
 * Weakstone's Subjugation's shape with an unconditional enter-tap.
 */
val Malfunction = card("Malfunction") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "When this Aura enters, tap enchanted permanent.\n" +
        "Enchanted permanent doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedPermanent)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Izzy"
        flavorText = "\"They don't make them like they used to.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c41f907f-4512-4c5d-827b-fef04613d641.jpg?1783937217"
    }
}
