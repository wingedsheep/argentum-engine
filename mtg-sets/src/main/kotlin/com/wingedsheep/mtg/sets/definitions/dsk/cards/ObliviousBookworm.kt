package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Oblivious Bookworm (DSK 225)
 * {G}{U}
 * Creature — Human Wizard
 * 2/3
 * At the beginning of your end step, you may draw a card. If you do, discard a card unless a
 * permanent entered the battlefield face down under your control this turn or you turned a
 * permanent face up this turn.
 *
 * The "unless ~" rider is modelled as a [ConditionalEffect] gating the discard on the *negation*
 * of the two turn-tracking facts — so the discard only happens when neither face-down event
 * occurred this turn. Both facts are backed by per-player turn trackers
 * ([Conditions.PermanentEnteredFaceDownThisTurn] / [Conditions.YouTurnedPermanentFaceUpThisTurn]).
 */
val ObliviousBookworm = card("Oblivious Bookworm") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Wizard"
    oracleText = "At the beginning of your end step, you may draw a card. If you do, discard a card unless a permanent entered the battlefield face down under your control this turn or you turned a permanent face up this turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = MayEffect(
            effect = IfYouDoEffect(
                action = Effects.DrawCards(1),
                ifYouDo = ConditionalEffect(
                    condition = Conditions.Not(
                        Conditions.Any(
                            Conditions.PermanentEnteredFaceDownThisTurn,
                            Conditions.YouTurnedPermanentFaceUpThisTurn,
                        )
                    ),
                    effect = Patterns.Hand.discardCards(1),
                ),
            ),
            descriptionOverride = "You may draw a card. If you do, discard a card unless a permanent entered the battlefield face down under your control this turn or you turned a permanent face up this turn.",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Josh Newton"
        flavorText = "\"Huh, the detector won't stop beeping. Must be on the fritz again.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7b4c50b-fe76-430d-8f96-208f15ca4cd7.jpg?1726286707"
    }
}
