package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Stromkirk Bloodthief
 * {2}{B}
 * Creature — Vampire Rogue
 * 2/2
 * At the beginning of your end step, if an opponent lost life this turn, put a +1/+1 counter on
 * target Vampire you control.
 *
 * Intervening-if end-step trigger ([Conditions.OpponentLostLifeThisTurn], CR 603.4 — checked both
 * on trigger and on resolution). Canonical printing (Innistrad: Midnight Hunt); Foundations reprint
 * is a Printing row in the fdn package.
 */
val StromkirkBloodthief = card("Stromkirk Bloodthief") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your end step, if an opponent lost life this turn, put a +1/+1 " +
        "counter on target Vampire you control."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        interveningIf = Conditions.OpponentLostLifeThisTurn
        val vampire = target(
            "target Vampire you control",
            // A bare tribal noun names *permanents* of that tribe, not creatures of it — the
            // reading the Assay differential settled across the corpus.
            TargetObject(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl()))
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, vampire)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Caroline Gariba"
        flavorText = "With House Voldaren ascendant, some members of the Stromkirk line took it upon " +
            "themselves to \"borrow\" from the blood tithes."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa819123-bf13-44ea-9a6e-06c8ab023e44.jpg?1782703653"
    }
}
