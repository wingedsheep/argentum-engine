package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mazemind Tome
 * {2}
 * Artifact — Book
 *
 * {T}, Put a page counter on this artifact: Scry 1.
 * {2}, {T}, Put a page counter on this artifact: Draw a card.
 * When there are four or more page counters on this artifact, exile it. If you do, you gain 4 life.
 *
 * The counter is an *accruing activation cost* ([Costs.PutCounterOnSelf]), not a resolution
 * effect — it is paid whether or not the ability resolves, and it takes nothing away, which is
 * what lets the fourth activation happen at all before the state trigger exiles the Tome.
 *
 * The third ability is state-triggered (CR 603.8): there is no event to hang it on, the engine
 * polls the counter count and fires on the false → true transition. The exile is scoped
 * `fromZone = BATTLEFIELD` so a Tome that has already left while the trigger sits on the stack
 * isn't dragged out of its new zone — and because `Effects.IfYouDo`'s `Auto` criterion measures the
 * exile zone's growth, a skipped exile also means no life (Scryfall ruling below).
 */
val MazemindTome = card("Mazemind Tome") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Book"
    oracleText = "{T}, Put a page counter on this artifact: Scry 1. (Look at the top card of your " +
        "library. You may put that card on the bottom.)\n" +
        "{2}, {T}, Put a page counter on this artifact: Draw a card.\n" +
        "When there are four or more page counters on this artifact, exile it. If you do, you gain 4 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PutCounterOnSelf(CounterType.PAGE))
        effect = Patterns.Library.scry(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.PutCounterOnSelf(CounterType.PAGE))
        effect = Effects.DrawCards(1)
    }

    stateTriggeredAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.PAGE, 4)
        effect = Effects.IfYouDo(
            action = Effects.Move(
                target = EffectTarget.Self,
                destination = Zone.EXILE,
                fromZone = Zone.BATTLEFIELD,
            ),
            then = Effects.GainLife(4, EffectTarget.Controller),
        )
        description = "When there are four or more page counters on this artifact, exile it. " +
            "If you do, you gain 4 life"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fd761f3-6b43-4150-8595-dc3abd85b06c.jpg?1783930659"
        ruling(
            "2020-06-23",
            "If Mazemind Tome leaves the battlefield while its triggered ability is on the stack, " +
                "you can't exile it from the zone it's put into."
        )
    }
}
