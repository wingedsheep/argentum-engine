package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

val Firebreathing = card("Firebreathing") {
    manaCost = "{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n{R}: Enchanted creature gets +1/+0 until end of turn."
    colorIdentity = "R"
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.EnchantedCreature)
    }

    metadata {
        ruling("2009-10-01", "This ability can be activated by Firebreathing’s controller, not the enchanted creature’s controller (in case they’re different players).")
        ruling("2009-10-01", "The ability affects whichever creature is enchanted by Firebreathing at the time the ability resolves. The bonus remains even if Firebreathing stops enchanting that creature.")
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3eb27381-505d-4e47-bf66-9e7ba91a5075.jpg?1783948686"
        flavorText = "\"And topples round the dreary west\nA looming bastion fringed with fire.\"\n—Alfred, Lord Tennyson, \"In Memoriam\""
    }
}
