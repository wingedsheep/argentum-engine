package com.wingedsheep.mtg.sets.definitions.isd.cards

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
 * Claustrophobia
 * {1}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 */
val Claustrophobia = card("Claustrophobia") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Ryan Pancoast"
        flavorText = "Six feet of earth muffled his cries."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7e5f600-4d19-42a4-b57e-650c76041798.jpg?1783940979"
        ruling("2015-06-22", "Claustrophobia can target and enchant a tapped or untapped creature.")
        ruling("2015-06-22", "The enchanted creature can still be untapped in other ways. Claustrophobia will remain attached, and the creature will continue to not untap during its controller's untap step.")
    }
}
