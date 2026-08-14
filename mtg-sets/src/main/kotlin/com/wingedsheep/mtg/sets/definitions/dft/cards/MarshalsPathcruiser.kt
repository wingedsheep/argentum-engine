package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marshals' Pathcruiser
 * {3}
 * Artifact — Vehicle
 * 6/5
 * When this Vehicle enters, search your library for a basic land card, reveal it, put it into
 * your hand, then shuffle.
 * Exhaust — {W}{U}{B}{R}{G}: This Vehicle becomes an artifact creature. Put two +1/+1 counters
 * on it. (Activate each exhaust ability only once.)
 * Crew 5
 *
 * The exhaust ability animates the Vehicle **permanently** (no "until end of turn" clause), so it
 * uses `Duration.Permanent` rather than the crew default — the same [Effects.BecomeCreature] shape
 * the engine's crew handler builds, with the Vehicle's printed 6/5 as the base P/T so the +1/+1
 * counters layer on top in 7d.
 */
val MarshalsPathcruiser = card("Marshals' Pathcruiser") {
    manaCost = "{3}"
    colorIdentity = "WUBRG"
    typeLine = "Artifact — Vehicle"
    oracleText = "When this Vehicle enters, search your library for a basic land card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "Exhaust — {W}{U}{B}{R}{G}: This Vehicle becomes an artifact creature. Put two +1/+1 " +
        "counters on it. (Activate each exhaust ability only once.)\n" +
        "Crew 5 (Tap any number of creatures you control with total power 5 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 6
    toughness = 5

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    activatedAbility {
        cost = Costs.Mana("{W}{U}{B}{R}{G}")
        isExhaust = true
        effect = Effects.Composite(
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 6,
                toughness = 5,
                duration = Duration.Permanent
            ),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        )
        description = "Exhaust — {W}{U}{B}{R}{G}: This Vehicle becomes an artifact creature. " +
            "Put two +1/+1 counters on it."
    }

    keywordAbility(KeywordAbility.crew(5))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f920f83-6380-4e4f-be68-6bf9df3110d8.jpg?1783907847"
    }
}
