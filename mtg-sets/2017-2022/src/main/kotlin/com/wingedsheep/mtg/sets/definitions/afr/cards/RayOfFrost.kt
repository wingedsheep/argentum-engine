package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ray of Frost — Adventures in the Forgotten Realms #68
 * {1}{U} · Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * When this Aura enters, if enchanted creature is red, tap it.
 * As long as enchanted creature is red, it loses all abilities.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * Flood the Engine's shape with both colour clauses gated on the host being red: the enters
 * trigger is an intervening-if over [Conditions.EnchantedPermanentMatches], and the ability loss
 * is a conditional static on the same check (read in projected state, so a colour-changing
 * effect turns it on or off). The untap lock is unconditional.
 */
val RayOfFrost = card("Ray of Frost") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, if enchanted creature is red, tap it.\n" +
        "As long as enchanted creature is red, it loses all abilities.\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature.withColor(Color.RED))
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, if enchanted creature is red, tap it."
    }

    staticAbility {
        ability = LoseAllAbilities()
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature.withColor(Color.RED))
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Kim Sokol"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7edd5058-a964-4288-937b-84df6e9ba7c4.jpg?1783926511"
    }
}
