package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Candlestick — Murders at Karlov Manor #43
 * {U} · Artifact — Clue Equipment
 *
 * Equipped creature gets +1/+1 and has "Whenever this creature attacks, surveil 2."
 * {2}, Sacrifice this Equipment: Draw a card.
 * Equip {2}
 *
 * One of the set's "murder weapon" Equipment — an Equipment that is also a Clue, so it carries the
 * standard Clue sacrifice-to-draw *and* the Clue subtype, which the set's "sacrifice a Clue"
 * payoffs read straight off the type line (Scryfall's ruling: "If an effect refers to a Clue, it
 * means any Clue artifact, not just a Clue artifact token").
 *
 * The attack trigger is granted to the equipped creature via [GrantTriggeredAbility] with
 * [Triggers.Attacks] (SELF binding), so it fires on that creature's attack rather than on any
 * attack — and it surveils for the *creature's* controller, which is what matters when the
 * Equipment's controller and the equipped creature's controller have diverged. Unequipping
 * before the trigger resolves doesn't fizzle it: the granted ability has already triggered and
 * exists independently on the stack (CR 603.2).
 */
val Candlestick = card("Candlestick") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Clue Equipment"
    oracleText = "Equipped creature gets +1/+1 and has \"Whenever this creature attacks, " +
        "surveil 2.\"\n" +
        "{2}, Sacrifice this Equipment: Draw a card.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(+1, +1, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.Attacks.event,
                binding = Triggers.Attacks.binding,
                effect = Patterns.Library.surveil(2)
            ),
            filter = Filters.EquippedCreature
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5aeae6fb-3834-4891-924e-3d1fb3e19e09.jpg?1783912915"
    }
}
