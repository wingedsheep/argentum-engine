package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Out of the Way
 * {3}{U}
 * Instant
 * This spell costs {2} less to cast if it targets a green permanent.
 * Return target nonland permanent an opponent controls to its owner's hand.
 * Draw a card.
 *
 * The discount is the Knockout Blow shape — [SpellCostTarget.SelfCast] over
 * [CostReductionSource.FixedIfAnyTargetMatches], keyed on a green-permanent filter.
 */
val OutOfTheWay = card("Out of the Way") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if it targets a green permanent.\nReturn target nonland permanent an opponent controls to its owner's hand.\nDraw a card."

    spell {
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.ReturnToHand(permanent) then Effects.DrawCards(1)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Permanent.withColor(Color.GREEN),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Tony Foti"
        flavorText = "\"You bore me.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2201ade5-8add-49f2-8045-ae351aaf061c.jpg?1783923143"
    }
}
