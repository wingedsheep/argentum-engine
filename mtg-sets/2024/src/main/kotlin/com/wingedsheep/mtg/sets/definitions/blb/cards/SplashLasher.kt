package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Splash Lasher
 * {3}{U}
 * Creature — Frog Wizard
 * 3/3
 *
 * Offspring {1}{U} (You may pay an additional {1}{U} as you cast this spell.
 * If you do, when this creature enters, create a 1/1 token copy of it.)
 *
 * When this creature enters, tap up to one target creature and put a stun
 * counter on it.
 *
 */
val SplashLasher = card("Splash Lasher") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Frog Wizard"
    power = 3
    toughness = 3
    oracleText = "Offspring {1}{U} (You may pay an additional {1}{U} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nWhen this creature enters, tap up to one target creature and put a stun counter on it."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.offspring("{1}{U}"))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // ETB: tap up to one target creature and put a stun counter on it
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature, optional = true)
        effect = Effects.Tap(t) then Effects.AddCounters(CounterType.STUN, 1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Brian Valeza"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/362ee125-35a0-46cd-a201-e6797d12d33a.jpg?1721426269"

        ruling("2024-07-26", "You can pay an offspring cost only once as you cast a spell with offspring.")
        ruling("2024-07-26", "If the spell is countered, the offspring ability will not trigger, and no token will be created.")
        ruling("2024-07-26", "The token copies exactly what was printed on the original creature and nothing else, except it's a 1/1.")
    }
}
