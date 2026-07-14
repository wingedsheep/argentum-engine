package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nicanzil, Current Conductor
 * {G}{U}
 * Legendary Creature — Merfolk Scout
 * 2/3
 *
 * Whenever a creature you control explores a land card, you may put a land card from your hand
 * onto the battlefield tapped.
 * Whenever a creature you control explores a nonland card, put a +1/+1 counter on Nicanzil.
 *
 * The two abilities split on the explore reveal outcome (CR 701.44a) via the LAND / NONLAND
 * variants of the explore trigger. The land drop reuses [Patterns.Hand.putFromHand] (choose up to
 * one, so the "you may" is inherent) with `entersTapped = true`.
 */
val NicanzilCurrentConductor = card("Nicanzil, Current Conductor") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Merfolk Scout"
    oracleText = "Whenever a creature you control explores a land card, you may put a land card " +
        "from your hand onto the battlefield tapped.\n" +
        "Whenever a creature you control explores a nonland card, put a +1/+1 counter on Nicanzil."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.WheneverCreatureYouControlExploresLand
        effect = Patterns.Hand.putFromHand(filter = GameObjectFilter.Land, entersTapped = true)
    }

    triggeredAbility {
        trigger = Triggers.WheneverCreatureYouControlExploresNonland
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Fariba Khamseh"
        flavorText = "\"This water can guide you anywhere, if you know how to ask.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e6f4aba-3500-4fb7-ab78-02f63c03778a.jpg?1782694423"
    }
}
