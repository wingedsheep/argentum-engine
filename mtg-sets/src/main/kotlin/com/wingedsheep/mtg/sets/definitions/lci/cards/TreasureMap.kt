package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Treasure Map // Treasure Cove (CR 712, The Lost Caverns of Ixalan; originally Ixalan)
 * {2}
 * Artifact // Land
 *
 * Front — Treasure Map (Artifact, {2})
 *   {1}, {T}: Scry 1. Put a landmark counter on this artifact. Then if there are three or
 *   more landmark counters on it, remove those counters, transform this artifact, and create
 *   three Treasure tokens.
 *
 * Back — Treasure Cove (Land)
 *   {T}: Add {C}.
 *   {T}, Sacrifice a Treasure: Draw a card.
 *
 * Implementation:
 *  - Front activated ability composes [Effects.Scry] (1) → [Effects.AddCounters] (a passive
 *    [Counters.LANDMARK] counter on Self) → a [ConditionalEffect] gated on
 *    [Conditions.SourceCounterCountAtLeast]`(landmark, 3)` that removes the three counters
 *    ([Effects.RemoveCounters]), flips the artifact ([TransformEffect]) and makes three
 *    Treasures ([Effects.CreateTreasure]). Same "add counter, then conditionally do more"
 *    shape as The Emperor of Palamecia.
 *  - Back is a [CardDefinition.doubleFacedPermanent] land with a `{T}: Add {C}` mana ability
 *    and a `{T}, Sacrifice a Treasure: Draw a card` ability
 *    (`Costs.Sacrifice(Artifact withSubtype "Treasure")`).
 */

private val TreasureMapFront = card("Treasure Map") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Scry 1. Put a landmark counter on this artifact. Then if there " +
        "are three or more landmark counters on it, remove those counters, transform this " +
        "artifact, and create three Treasure tokens. (To scry 1, look at the top card of your " +
        "library, then you may put that card on the bottom of your library.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Composite(
            Effects.Scry(1),
            Effects.AddCounters(Counters.LANDMARK, 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast(Counters.LANDMARK, 3),
                effect = Effects.Composite(
                    Effects.RemoveCounters(Counters.LANDMARK, 3, EffectTarget.Self),
                    TransformEffect(EffectTarget.Self),
                    Effects.CreateTreasure(3),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a924fe1e-a85e-4e14-88d2-ac55130638ab.jpg?1782694398"
    }
}

private val TreasureCove = card("Treasure Cove") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{T}, Sacrifice a Treasure: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Artifact.withSubtype("Treasure")),
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/back/a/9/a924fe1e-a85e-4e14-88d2-ac55130638ab.jpg?1782694398"
    }
}

val TreasureMap: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TreasureMapFront,
    backFace = TreasureCove,
)
