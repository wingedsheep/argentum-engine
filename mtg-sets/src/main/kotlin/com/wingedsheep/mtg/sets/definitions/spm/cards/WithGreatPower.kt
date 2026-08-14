package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.RedirectDamage
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * With Great Power . . . — Marvel's Spider-Man #24
 * {3}{W} · Enchantment — Aura
 *
 * Enchant creature you control
 * Enchanted creature gets +2/+2 for each Aura and Equipment attached to it.
 * All damage that would be dealt to you is dealt to enchanted creature instead.
 *
 * The redirect is a Pariah-style static [RedirectDamage] to `EffectTarget.EnchantedCreature`
 * (now resolved by `DamageUtils.resolveRedirectTarget`).
 */
val WithGreatPower = card("With Great Power . . .") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "Enchanted creature gets +2/+2 for each Aura and Equipment attached to it.\n" +
        "All damage that would be dealt to you is dealt to enchanted creature instead."

    auraTarget = Targets.CreatureYouControl

    // Enchanted creature gets +2/+2 for each Aura and Equipment attached to it.
    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmount.Multiply(DynamicAmounts.attachmentsOnEnchantedCreature(), 2),
            toughnessBonus = DynamicAmount.Multiply(DynamicAmounts.attachmentsOnEnchantedCreature(), 2)
        )
    }

    // All damage that would be dealt to you is dealt to enchanted creature instead.
    replacementEffect(
        RedirectDamage(
            redirectTo = EffectTarget.EnchantedCreature,
            appliesTo = EventPattern.DamageEvent(recipient = RecipientFilter.You)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "E. M. Gist"
        flavorText = ". . . there must also come great responsibility!"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f717c096-e161-426e-a8d7-c93b117e16b9.jpg?1783905357"
    }
}
