package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * There and Back Again
 * {3}{R}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Up to one target creature can't block for as long as you control this Saga.
 *     The Ring tempts you.
 * II — Search your library for a Mountain card, put it onto the battlefield, then shuffle.
 * III — Create Smaug, a legendary 6/6 red Dragon creature token with flying, haste, and
 *       "When Smaug dies, create fourteen Treasure tokens."
 *
 * Chapter I uses [Duration.WhileYouControlSource] for the floating "can't block" effect:
 * if an opponent Threatens the Saga itself, the target creature recovers its ability to
 * block immediately, which is what "as long as you control this Saga" actually means. The
 * tempt rider rides along regardless of whether the optional target was chosen.
 *
 * Chapter II tutors via [Patterns.Library.searchLibrary] for any "Mountain" card (basic or
 * snow Mountain, Mountain dual lands such as Stomping Ground), onto the battlefield.
 *
 * Chapter III spawns Smaug as a one-off named legendary 6/6 red Dragon token. The death
 * rider is granted on the token via [CreateTokenEffect.triggeredAbilities] — a
 * `Triggers.Dies` event with [Effects.CreateTreasure]`(14)` as the payoff.
 */
val ThereAndBackAgain = card("There and Back Again") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Up to one target creature can't block for as long as you control this Saga. The Ring tempts you.\n" +
        "II — Search your library for a Mountain card, put it onto the battlefield, then shuffle.\n" +
        "III — Create Smaug, a legendary 6/6 red Dragon creature token with flying, haste, and \"When Smaug dies, create fourteen Treasure tokens.\""

    sagaChapter(1) {
        val creature = target(
            "up to one target creature",
            TargetCreature(optional = true)
        )
        effect = CompositeEffect(listOf(
            Effects.CantBlock(creature, Duration.WhileYouControlSource("There and Back Again")),
            Effects.TheRingTemptsYou()
        ))
    }

    sagaChapter(2) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype("Mountain"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD
        )
    }

    sagaChapter(3) {
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(1),
            power = 6,
            toughness = 6,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING, Keyword.HASTE),
            name = "Smaug",
            legendary = true,
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.Dies.event,
                    binding = Triggers.Dies.binding,
                    effect = Effects.CreateTreasure(14)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/939b0bd0-24ea-45de-a2d3-37bbf6a3e6f9.jpg?1689938360"
    }
}
