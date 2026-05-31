package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Watcher of the Wayside — Tarkir: Dragonstorm #249
 * {3} · Artifact Creature — Golem · 3/2
 *
 * When this creature enters, target player mills two cards. You gain 2 life.
 *
 * A single enters trigger with a player target. The mill is directed at the chosen target player
 * (`EffectTarget.ContextTarget(0)`); the life gain is unconditional and goes to the controller
 * (`EffectTarget.Controller`), so it happens whether or not the target had cards to mill. Composed
 * from the `EffectPatterns.mill` Gather→Move pipeline + `Effects.GainLife`.
 */
val WatcherOfTheWayside = card("Watcher of the Wayside") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Golem"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, target player mills two cards. You gain 2 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("target player", Targets.Player)
        effect = Effects.Composite(
            EffectPatterns.mill(2, EffectTarget.ContextTarget(0)),
            Effects.GainLife(2, EffectTarget.Controller)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "249"
        artist = "Brian Valeza"
        flavorText = "Against dragonlords, khans, and Ugin himself, the golem survived. Original purpose forgotten, it awaits orders that will never come, from a world that has moved on without it."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dcafdac-a293-4adc-a540-3b3f469cf6f3.jpg?1743204983"
    }
}
