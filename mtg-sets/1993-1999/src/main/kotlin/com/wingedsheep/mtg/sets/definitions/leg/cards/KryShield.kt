package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kry Shield
 * {2}
 * Artifact
 *
 * {2}, {T}: Prevent all damage that would be dealt this turn by target creature you control.
 * That creature gets +0/+X until end of turn, where X is its mana value.
 */
val KryShield = card("Kry Shield") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Prevent all damage that would be dealt this turn by target creature you control. " +
        "That creature gets +0/+X until end of turn, where X is its mana value."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.PreventAllDamageDealtBy(creature) then
            Effects.ModifyStats(DynamicAmounts.fixed(0), DynamicAmounts.manaValueOf(creature), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "282"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a558f23c-c2ce-40d0-b894-f8ccbff8f622.jpg?1783948027"
    }
}
