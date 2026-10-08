package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.SkipStepOrPhase
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode

/**
 * Colfenor's Plans
 * {2}{B}{B}
 * Enchantment
 *
 * When this enchantment enters, exile the top seven cards of your library face down.
 * You may look at the cards exiled with this enchantment, and you may play lands and cast
 * spells from among those cards.
 * Skip your draw step.
 * You can't cast more than one spell each turn.
 *
 * Four printed lines, four pieces:
 *
 *  - **The ETB is a gather → move**, with `linkToSource = true` so the seven cards become *this*
 *    enchantment's pile. That link is what the play permission below reads; without it the cards
 *    would be ordinary face-down exile that nobody can ever see or use.
 *  - **"You may look at" and "you may play" are two separate grants** —
 *    `MoveCollectionEffect.lookableInExile` and [GrantMayCastFromLinkedExile]. Exile gives a
 *    face-down card's own controller no baseline visibility (CR 708.5), so dropping the look grant
 *    would leave you playing blind out of your own pile; and the look grant alone confers no
 *    permission to play. See Jacob Hauken, Inspector for the pair in its original form.
 *  - **`filter = Any` is what admits the lands.** A land is played, never cast (CR 305.1), and the
 *    grant's land leg is the same permission as its cast leg — which is exactly the printed "play
 *    lands **and** cast spells". `withoutPayingManaCost` stays off: this card's pile is played at
 *    full price, unlike hideaway's.
 *  - **[SkipStepOrPhase] (`DRAW_STEP`)** is the standing counterpart of the engine's one-shot skip marker, added
 *    for this card. It is read as the draw step begins rather than projected, because a skipped
 *    draw step is a turn-based action.
 *
 * The one-spell-per-turn clause is [RestrictSpellsCastPerTurn] scoped to the controller — Rule of
 * Law's ability with `eachPlayer = false`, the Yawgmoth's Agenda shape. Note it binds *every*
 * spell you cast, not just the ones out of the pile.
 */
val ColfenorsPlans = card("Colfenor's Plans") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile the top seven cards of your library face " +
        "down.\n" +
        "You may look at the cards exiled with this enchantment, and you may play lands and cast " +
        "spells from among those cards.\n" +
        "Skip your draw step.\n" +
        "You can't cast more than one spell each turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val colfenorsPlansExiled = gather(CardSource.TopOfLibrary(7))
            move(
                colfenorsPlansExiled,
                CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN,
                linkToSource = true,
                lookableInExile = true
            )
        }
        description = "When this enchantment enters, exile the top seven cards of your library " +
            "face down."
    }

    staticAbility {
        ability = GrantMayCastFromLinkedExile(filter = GameObjectFilter.Any)
    }

    staticAbility {
        ability = SkipStepOrPhase(TurnPart.DRAW_STEP)
    }

    staticAbility {
        ability = RestrictSpellsCastPerTurn(maxPerTurn = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Darrell Riche"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8d06368-d226-4089-84d4-950a3ebdfb15.jpg?1783942893"
        ruling(
            "2007-10-01",
            "The turn you cast Colfenor's Plans, you have cast (at least) one spell that turn. " +
                "After it enters and its last ability goes into effect, you can't cast any more " +
                "spells that turn."
        )
        ruling(
            "2007-10-01",
            "Playing a card exiled with Colfenor's Plans follows all the normal rules for playing " +
                "that card. You must pay its costs, and you must follow all timing restrictions, " +
                "for example."
        )
        ruling("2007-10-01", "If an exiled card has morph, you may cast it face down.")
        ruling(
            "2013-04-15",
            "If Colfenor's Plans leaves the battlefield, you can continue to look at cards exiled " +
                "by it, but you can no longer play them."
        )
    }
}
