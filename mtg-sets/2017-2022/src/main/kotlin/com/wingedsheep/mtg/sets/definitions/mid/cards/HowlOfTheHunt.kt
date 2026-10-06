package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Howl of the Hunt
 * {2}{G}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * When this Aura enters, if enchanted creature is a Wolf or Werewolf, untap that creature.
 * Enchanted creature gets +2/+2 and has vigilance.
 *
 * "If enchanted creature is a Wolf or Werewolf" is an intervening-if (checked when the trigger
 * would fire and again on resolution), read off projected subtypes via `withAnySubtype`.
 */
val HowlOfTheHunt = card("Howl of the Hunt") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "When this Aura enters, if enchanted creature is a Wolf or Werewolf, untap that creature.\n" +
        "Enchanted creature gets +2/+2 and has vigilance."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.EnchantedPermanentMatches(
            GameObjectFilter.Creature.withAnySubtype("Wolf", "Werewolf")
        )
        effect = Effects.Untap(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, if enchanted creature is a Wolf or Werewolf, untap that creature."
    }

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a48a42e-5cc5-4f9a-8745-99936f4cae5f.jpg?1783925578"
    }
}
