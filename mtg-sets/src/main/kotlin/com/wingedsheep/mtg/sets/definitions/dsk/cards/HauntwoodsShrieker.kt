package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Hauntwoods Shrieker
 * {1}{G}{G}
 * Creature — Beast Mutant
 * 3/3
 *
 * Whenever this creature attacks, manifest dread. (Look at the top two cards of your library. Put
 * one onto the battlefield face down as a 2/2 creature and the other into your graveyard. Turn it
 * face up any time for its mana cost if it's a creature card.)
 * {1}{G}: Reveal target face-down permanent. If it's a creature card, you may turn it face up.
 *
 * The attack trigger reuses the shared [Patterns.Library.manifestDread] recipe (CR 701.62).
 *
 * The activated ability targets *any* face-down permanent (any controller). It reveals the hidden
 * card, then — gated on the underlying card actually being a creature card via
 * [Conditions.TargetIsCreatureCard] (which reads the base card type, not the face-down 2/2
 * projection) — offers an optional ([MayEffect]) free flip via [TurnFaceUpEffect]. The reveal and
 * the flip are decoupled: a non-creature card is revealed but never flipped.
 */
val HauntwoodsShrieker = card("Hauntwoods Shrieker") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast Mutant"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks, manifest dread. (Look at the top two cards of " +
        "your library. Put one onto the battlefield face down as a 2/2 creature and the other into " +
        "your graveyard. Turn it face up any time for its mana cost if it's a creature card.)\n" +
        "{1}{G}: Reveal target face-down permanent. If it's a creature card, you may turn it face up."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Library.manifestDread()
    }

    // {1}{G}: Reveal target face-down permanent. If it's a creature card, you may turn it face up.
    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        val t = target(
            "target",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.faceDown()))
        )
        effect = Effects.Composite(
            Effects.RevealFaceDownPermanent(t),
            ConditionalEffect(
                condition = Conditions.TargetIsCreatureCard(0),
                effect = MayEffect(TurnFaceUpEffect(t)),
            ),
        )
        description = "{1}{G}: Reveal target face-down permanent. If it's a creature card, you may " +
            "turn it face up."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "182"
        artist = "John Tedrick"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/744ef0bc-9973-450e-a0c4-056d8244f357.jpg?1726286542"
    }
}
