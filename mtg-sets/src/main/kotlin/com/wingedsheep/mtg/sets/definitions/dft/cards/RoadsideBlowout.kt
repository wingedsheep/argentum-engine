package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Roadside Blowout — Aetherdrift #58
 * {2}{U} · Sorcery
 *
 * This spell costs {2} less to cast if it targets a permanent with mana value 1.
 * Return target creature or Vehicle an opponent controls to its owner's hand.
 * Draw a card.
 *
 * Same shape as Ride's End: the lone "creature or Vehicle" target is itself the permanent the
 * discount inspects, so [CostReductionSource.FixedIfAnyTargetMatches] over a mana-value-1
 * permanent filter resolves at cast time once the target is announced (CR 601.2f).
 * Per the 2025-02-07 ruling, a permanent whose mana cost contains {X} has X = 0 there — that
 * falls out of the engine's stored mana value, which is already the on-battlefield value.
 *
 * The draw is not conditional on the bounce resolving: if the target becomes illegal the whole
 * spell fizzles, but a target that is still legal and simply cannot be moved still leaves the
 * draw intact, so the two effects sit in a plain [Effects.Composite].
 */
val RoadsideBlowout = card("Roadside Blowout") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "This spell costs {2} less to cast if it targets a permanent with mana value 1.\n" +
        "Return target creature or Vehicle an opponent controls to its owner's hand.\n" +
        "Draw a card."

    spell {
        val t = target(
            "target creature or Vehicle an opponent controls",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.CreatureOrVehicle.opponentControls()),
            ),
        )
        effect = Effects.Composite(
            Effects.ReturnToHand(t),
            Effects.DrawCards(1),
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Permanent.manaValue(1),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Michele Giorgi"
        flavorText = "\"In cases of extreme emergency, just wing it.\"\n—Rocketeer safety manual, full text"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6153a76-56f7-46ee-bba5-b62c0143388a.jpg?1783907905"
        ruling("2025-02-07", "If there's an {X} in a permanent's mana cost, X is 0 when determining its mana value.")
    }
}
