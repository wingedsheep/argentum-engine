package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lightning Axe
 * {R}
 * Instant
 *
 * As an additional cost to cast this spell, discard a card or pay {5}.
 * Lightning Axe deals 5 damage to target creature.
 *
 * The binary additional cost (discard a card OR pay {5}) is modeled with [ModalEffect.chooseOne]
 * — the same fork Bitter Triumph uses for "discard a card or pay 3 life". Both modes resolve the
 * same spell effect (5 damage to target creature); they differ only in the additional cost paid
 * as the spell is cast. `countsAsModalSpell = false` because this is not a "Choose one — •" modal
 * spell, so "whenever you cast a modal spell" triggers must not see it.
 */
val LightningAxe = card("Lightning Axe") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, discard a card or pay {5}.\n" +
        "Lightning Axe deals 5 damage to target creature."

    spell {
        effect = ModalEffect.chooseOne(
            // Discard a card
            Mode(
                effect = Effects.DealDamage(5, EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(Targets.Creature),
                description = "Discard a card — deal 5 damage to target creature",
                additionalCosts = listOf(Costs.additional.DiscardCards(count = 1))
            ),
            // Pay {5}
            Mode(
                effect = Effects.DealDamage(5, EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(Targets.Creature),
                description = "Pay {5} — deal 5 damage to target creature",
                additionalManaCost = "{5}"
            ),
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Dan Murayama Scott"
        flavorText = "\"A gargoyle's meat can be carved with an ordinary cleaver, but for its petrous hide . . .\"\n—Asmoranomardicadaistinaculdacar, The Underworld Cookbook"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b748290-04b0-48a9-81aa-ab43182cf339.jpg?1783943219"

        ruling("2021-03-19", "The mana value of Lightning Axe is 1, no matter which additional cost you paid.")
    }
}
