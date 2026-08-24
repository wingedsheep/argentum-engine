package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thrull Retainer
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * Sacrifice this Aura: Regenerate enchanted creature.
 *
 * The regeneration shield is set up by an ability that sacrifices the Aura to pay for itself, so
 * the +1/+1 is already gone by the time the shield is used — but the shield itself survives, since
 * it is a floating replacement effect on the creature and not an ability of the Aura.
 */
val ThrullRetainer = card("Thrull Retainer") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1.\n" +
        "Sacrifice this Aura: Regenerate enchanted creature."
    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = RegenerateEffect(EffectTarget.EnchantedPermanent)
        description = "Sacrifice this Aura: Regenerate enchanted creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Ron Spencer"
        flavorText = "\"Until the Rebellion, Thrulls served their masters faithfully—even at the cost of their own lives.\"\n—*Sarpadian Empires, vol. II*"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d800512b-1492-41d2-931d-57c625044454.jpg?1783947898"
    }
}
