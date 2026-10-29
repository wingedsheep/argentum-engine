package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Garruk, Unleashed
 * {2}{G}{G}
 * Legendary Planeswalker — Garruk
 * Loyalty: 4
 *
 * +1: Up to one target creature gets +3/+3 and gains trample until end of turn.
 * −2: Create a 3/3 green Beast creature token. Then if an opponent controls more creatures than
 * you, put a loyalty counter on Garruk.
 * −7: You get an emblem with "At the beginning of your end step, you may search your library for
 * a creature card, put it onto the battlefield, then shuffle."
 *
 * The −2's comparison is made after the Beast is created ("Then if …"), and adds exactly one
 * loyalty counter however many opponents outnumber you (ruling). The emblem is a permanent global
 * triggered ability; its "you may" is the optional gate, so declining skips the search and the
 * shuffle.
 */
val GarrukUnleashed = card("Garruk, Unleashed") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Garruk"
    startingLoyalty = 4
    oracleText = "+1: Up to one target creature gets +3/+3 and gains trample until end of turn.\n" +
        "−2: Create a 3/3 green Beast creature token. Then if an opponent controls more creatures " +
        "than you, put a loyalty counter on Garruk.\n" +
        "−7: You get an emblem with \"At the beginning of your end step, you may search your library " +
        "for a creature card, put it onto the battlefield, then shuffle.\""

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(3, 3, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Beast"),
            imageUri = "https://cards.scryfall.io/normal/front/8/8/882247ba-99d2-46db-8314-f800f3366b7f.jpg?1783930590"
        ) then Effects.If(
            condition = Conditions.OpponentControlsMoreCreatures,
            then = Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.Self)
        )
    }

    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.beginningOf(Step.END)
                optional = true
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Creature,
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    shuffleAfter = true
                )
            },
            descriptionOverride = "At the beginning of your end step, you may search your library " +
                "for a creature card, put it onto the battlefield, then shuffle."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "183"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af2fdbec-bca2-4af5-9c2a-28b0b35b18a3.jpg?1783930676"
        ruling("2020-06-23", "Garruk's second ability gives him only one loyalty counter, no matter how many opponents control how many more creatures than you do.")
        ruling("2020-06-23", "If Garruk has only two loyalty counters, activating his second loyalty ability results in him dying before it resolves. He won't be there to get a loyalty counter.")
        ruling("2020-06-23", "If the creature you put onto the battlefield with Garruk's emblem's ability during your end step has an ability that triggers at the beginning of your end step, it won't trigger during that end step.")
    }
}
