package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.SetName
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Honest Work
 * {U}
 * Enchantment — Aura
 *
 * Enchant creature an opponent controls
 * When this Aura enters, tap enchanted creature and remove all counters from it.
 * Enchanted creature loses all abilities and is a Citizen with base power and toughness 1/1
 * and "{T}: Add {C}" named Humble Merchant. (It loses all other creature types and names.)
 *
 * The "becomes a 1/1 X named Y with '...'" composite mirrors Sugar Coat
 * (TransformPermanent + LoseAllAbilities + GrantActivatedAbility), adding the new Layer-3
 * SetName static for the fixed-name override (CR 612 / 613.1c) and a base-P/T set.
 */
val HonestWork = card("Honest Work") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature an opponent controls\nWhen this Aura enters, tap enchanted creature and remove all counters from it.\nEnchanted creature loses all abilities and is a Citizen with base power and toughness 1/1 and \"{T}: Add {C}\" named Humble Merchant. (It loses all other creature types and names.)"

    auraTarget = Targets.CreatureOpponentControls

    // "When this Aura enters, tap enchanted creature and remove all counters from it."
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
            .then(Effects.RemoveAllCounters(EffectTarget.EnchantedCreature))
    }

    // "is a Citizen ..." — keep the Creature type, replace all creature subtypes with just Citizen
    // (Layer 4). This also strips its other creature types/names alongside the SetName override.
    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("CREATURE"),
            setSubtypes = setOf("Citizen")
        )
    }

    // "with base power and toughness 1/1" — Layer 7b
    staticAbility {
        ability = SetBasePowerToughnessStatic(1, 1)
    }

    // "loses all abilities" — Layer 6
    staticAbility {
        ability = LoseAllAbilities()
    }

    // "named Humble Merchant" — Layer 3 (TEXT) fixed-name override
    staticAbility {
        ability = SetName("Humble Merchant")
    }

    // "with '{T}: Add {C}'" — Layer 6 granted mana ability
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.AddColorlessMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                descriptionOverride = "{T}: Add {C}."
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "ikeda_cpt"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57e0ccb0-22f0-4e4d-9a47-c4fec2c7f251.jpg?1764120279"
    }
}
