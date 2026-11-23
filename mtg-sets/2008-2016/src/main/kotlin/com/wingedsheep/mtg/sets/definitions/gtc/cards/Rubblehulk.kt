package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rubblehulk — Gatecrash #191 (canonical printing)
 * {4}{R}{G} · Creature — Elemental · * / *
 *
 * Rubblehulk's power and toughness are each equal to the number of lands you control.
 * Bloodrush — {1}{R}{G}, Discard this card: Target attacking creature gets +X/+X until end of turn,
 * where X is the number of lands you control.
 */
val Rubblehulk = card("Rubblehulk") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Elemental"
    oracleText = "Rubblehulk's power and toughness are each equal to the number of lands you control.\n" +
        "Bloodrush — {1}{R}{G}, Discard this card: Target attacking creature gets +X/+X until end of turn, where X is the number of lands you control."

    dynamicStats(DynamicAmounts.landsYouControl())

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(
            DynamicAmounts.landsYouControl(),
            DynamicAmounts.landsYouControl(),
            creature
        )
        description = "Bloodrush — {1}{R}{G}, Discard this card: Target attacking creature gets +X/+X until end of turn, where X is the number of lands you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c502590-f780-4512-8067-7c66f16f8c9d.jpg?1783940102"
    }
}
