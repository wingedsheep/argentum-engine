package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * This Town Ain't Big Enough
 * {4}{U}
 * Instant
 *
 * This spell costs {3} less to cast if it targets a permanent you control.
 * Return up to two target nonland permanents to their owners' hands.
 *
 * The cost reduction is a [SpellCostTarget.SelfCast] / [CostReductionSource.FixedIfAnyTargetMatches]
 * pair: {3} comes off if any chosen target is a permanent you control. The bounce is up-to-two
 * independent optional targets, returned in order.
 */
val ThisTownAintBigEnough = card("This Town Ain't Big Enough") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {3} less to cast if it targets a permanent you control.\n" +
        "Return up to two target nonland permanents to their owners' hands."

    spell {
        // "Return up to two target nonland permanents" — one up-to-two target requirement; the bounce
        // visits each chosen target, routing each permanent to its owner's hand.
        target("target", TargetPermanent(optional = true, count = 2, filter = TargetFilter.NonlandPermanent))
        effect = ForEachTargetEffect(listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)))
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 3,
                    filter = GameObjectFilter.Permanent.youControl(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Andrew Mar"
        flavorText = "\"All that fancy footwork, and you can't even dodge one silly little rope? " +
            "Cryin' shame, that is.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb206e27-da4d-4abe-9d8c-6d18c5f2f52a.jpg?1752091373"
    }
}
