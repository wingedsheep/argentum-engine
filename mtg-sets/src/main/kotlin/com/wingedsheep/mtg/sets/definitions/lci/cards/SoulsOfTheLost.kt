package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Souls of the Lost — {1}{B}
 * Creature — Spirit
 * Rare — The Lost Caverns of Ixalan #121
 * Artist: Nils Hamm
 *
 * "As an additional cost to cast this spell, discard a card or sacrifice a permanent.
 *  Fathomless descent — Souls of the Lost's power is equal to the number of permanent cards in
 *  your graveyard and its toughness is equal to that number plus 1."
 *
 * Two independent mechanics:
 *  - **Cost-vs-cost additional cost** — the caster pays *either* "discard a card" *or* "sacrifice a
 *    permanent", modeled with the general `Costs.additional.Choice` primitive
 *    ([com.wingedsheep.sdk.scripting.AdditionalCost.Choice]). Each option surfaces as its own cast
 *    action, so the player chooses the sub-cost by choosing which action to play.
 *  - **Fathomless descent CDA** — a characteristic-defining ability (Layer 7b) setting base power to
 *    the number of permanent cards in your graveyard and base toughness to that number plus one,
 *    reusing [DynamicAmount.Count]`(You, GRAVEYARD, Permanent)` (the same count Squirming Emergence /
 *    Song of Stupefaction read) via the `dynamicStats` DSL with a +1 toughness offset. The printed
 *    star/star P/T is replaced by these dynamic values.
 */
val SoulsOfTheLost = card("Souls of the Lost") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    oracleText = "As an additional cost to cast this spell, discard a card or sacrifice a permanent.\n" +
        "Fathomless descent — Souls of the Lost's power is equal to the number of permanent cards " +
        "in your graveyard and its toughness is equal to that number plus 1."

    dynamicStats(
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Permanent),
        toughnessOffset = 1
    )

    additionalCost(
        Costs.additional.Choice(
            Costs.additional.DiscardCards(),
            Costs.additional.SacrificePermanent()
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Nils Hamm"
        flavorText = "\"Do you remember us?\""
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5c6f502-f11f-4e41-beb8-0843432fa431.jpg?1782694513"
    }
}
