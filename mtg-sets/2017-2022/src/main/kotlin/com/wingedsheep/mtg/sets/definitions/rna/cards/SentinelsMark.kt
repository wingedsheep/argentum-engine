package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sentinel's Mark
 * {1}{W}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature gets +1/+2 and has vigilance.
 * Addendum — When this Aura enters, if you cast it during your main phase, enchanted creature
 * gains lifelink until end of turn.
 *
 * The addendum is an intervening-if on the Aura's own enters trigger: [Conditions.WasCast] keeps
 * an Aura put onto the battlefield without being cast from triggering, and
 * [Conditions.IsYourMainPhase] matches the cast-time phase, since the Aura resolves (and its
 * trigger resolves) before the phase it was cast in can end.
 */
val SentinelsMark = card("Sentinel's Mark") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets +1/+2 and has vigilance.\n" +
        "Addendum — When this Aura enters, if you cast it during your main phase, enchanted creature gains lifelink until end of turn."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase)
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.EnchantedCreature)
        description = "Addendum — When this Aura enters, if you cast it during your main phase, " +
            "enchanted creature gains lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6f4f661-ee4b-4fa5-99c4-106731d117ce.jpg?1783933718"
    }
}
