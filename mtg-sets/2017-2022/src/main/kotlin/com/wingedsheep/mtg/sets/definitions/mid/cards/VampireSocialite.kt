package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vampire Socialite
 * {B}{R}
 * Creature — Vampire Noble
 * 2/2
 *
 * Menace
 * When this creature enters, if an opponent lost life this turn, put a +1/+1 counter on each other
 * Vampire you control.
 * As long as an opponent lost life this turn, each other Vampire you control enters with an
 * additional +1/+1 counter on it.
 *
 * The ETB is an intervening-if ([Conditions.OpponentLostLifeThisTurn]) over
 * [Effects.ForEachInGroup] with `excludeSelf` for "each other". The static is an
 * [EntersWithCounters] replacement consulted from the battlefield: `otherOnly = true` keeps the
 * Socialite from countering itself, and its `condition` is evaluated with the Socialite's
 * controller as "you", so "an opponent" means the Socialite controller's opponents.
 *
 * A bare tribal noun names *permanents* of that tribe, as on Stromkirk Bloodthief.
 */
val VampireSocialite = card("Vampire Socialite") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Vampire Noble"
    power = 2
    toughness = 2
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, if an opponent lost life this turn, put a +1/+1 counter on each " +
        "other Vampire you control.\n" +
        "As long as an opponent lost life this turn, each other Vampire you control enters with an " +
        "additional +1/+1 counter on it."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.OpponentLostLifeThisTurn
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl(),
                excludeSelf = true,
            ),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "When this creature enters, if an opponent lost life this turn, put a +1/+1 " +
            "counter on each other Vampire you control."
    }

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            otherOnly = true,
            condition = Conditions.OpponentLostLifeThisTurn,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Suzanne Helmigh"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f07cbf04-2864-4407-800e-4d55c10d3426.jpg?1783925550"
    }
}
