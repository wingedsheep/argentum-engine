package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CanAttackAsThoughHasty
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

// Oracle grants permission to attack immediately, rather than haste's tap/untap-cost permission.
val InstillEnergy = card("Instill Energy") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature can attack as though it had haste.\n{0}: Untap enchanted creature. Activate only during your turn and only once each turn."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CanAttackAsThoughHasty(filter = Filters.EnchantedCreature)
    }

    activatedAbility {
        cost = Costs.Mana("{0}")
        effect = Effects.Untap(EffectTarget.EnchantedCreature)
        restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn, ActivationRestriction.OncePerTurn)
        description = "Untap enchanted creature"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bd38716-874c-4e3c-a315-837839a6258c.jpg?1783948676"
        ruling("2005-08-01", "Any Auras (or other effects) which are on the creature that would cause it to not be untapped (or have a cost to be untapped) during untap step do not in any way hinder or imply a cost to use this card's ability to untap once during the turn.")
        ruling("2004-10-04", "If attached to an opponent's creature, you can untap their creature during your turn.")
        ruling("2004-10-04", "Instill Energy's untap ability will not untap the creature until it resolves. This means other spells and abilities can be used before it actually becomes untapped.")
    }
}
