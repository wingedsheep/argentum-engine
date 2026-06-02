package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDrawAmount
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Quantum Riddler
 * {3}{U}{U}
 * Creature — Sphinx
 * 4/6
 *
 * Flying
 * When this creature enters, draw a card.
 * As long as you have one or fewer cards in hand, if you would draw one or more cards,
 * you draw that many cards plus one instead.
 * Warp {1}{U}
 *
 * The draw-replacement static is expressed as a [ModifyDrawAmount] gated on a hand-size
 * restriction. The engine consults this at the draw instruction's announcement site
 * (CR 121.2a) — `DrawCardsExecutor.execute` for spell/ability draws and
 * `DrawPhaseManager.performDrawStep` for the draw step — so the modifier fires once per
 * draw instruction and is not re-applied when the per-card loop pauses and resumes.
 */
val QuantumRiddler = card("Quantum Riddler") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 4
    toughness = 6
    oracleText = "Flying\n" +
        "When this creature enters, draw a card.\n" +
        "As long as you have one or fewer cards in hand, if you would draw one or more cards, " +
        "you draw that many cards plus one instead.\n" +
        "Warp {1}{U} (You may cast this card from your hand for its warp cost. Exile this " +
        "creature at the beginning of the next end step, then you may cast it from exile on a " +
        "later turn.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
        description = "When this creature enters, draw a card."
    }

    replacementEffect(
        ModifyDrawAmount(
            modifier = 1,
            restrictions = listOf(Conditions.CardsInHandAtMost(1)),
            appliesTo = EventPattern.DrawEvent(player = Player.You),
        )
    )

    warp = "{1}{U}"

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "72"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/120be808-ff3b-4fca-96a1-4db6b9825856.jpg?1752946843"
    }
}
