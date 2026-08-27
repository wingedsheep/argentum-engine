package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Herald of the Pantheon
 * {1}{G}
 * Creature — Centaur Shaman
 * 2/2
 * Enchantment spells you cast cost {1} less to cast.
 * Whenever you cast an enchantment spell, you gain 1 life.
 */
val HeraldOfThePantheon = card("Herald of the Pantheon") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Shaman"
    power = 2
    toughness = 2
    oracleText = "Enchantment spells you cast cost {1} less to cast.\nWhenever you cast an enchantment spell, you gain 1 life."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Enchantment),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    triggeredAbility {
        trigger = Triggers.YouCastEnchantment
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Jason A. Engle"
        flavorText = "The distinction of bearing the gods' banner is nothing compared to the glory of being closer to Nyx."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffeca7a7-2a14-4166-9e89-0f4eb94b79f5.jpg?1783938322"

        ruling("2015-06-22", "Herald of the Pantheon's first ability can't reduce the colored mana requirement of an enchantment spell.")
        ruling("2015-06-22", "If there are additional costs to cast an enchantment spell, apply those before applying cost reductions.")
        ruling("2015-06-22", "Herald of the Pantheon can reduce alternative costs such as bestow costs.")
    }
}
