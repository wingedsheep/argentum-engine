package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marang River Skeleton
 * {1}{B}
 * Creature — Skeleton
 * 1/1
 * {B}: Regenerate this creature.
 * Megamorph {3}{B}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost.
 */
val MarangRiverSkeleton = card("Marang River Skeleton") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton"
    power = 1
    toughness = 1
    oracleText = "{B}: Regenerate this creature.\n" +
        "Megamorph {3}{B} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    // Megamorph {3}{B}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{3}{B}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Jack Wang"
        flavorText = "The gurgling of the Marang conceals both footsteps and screams."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/9497f7e6-2121-44a9-9bd6-258685636b1c.jpg?1783938596"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
