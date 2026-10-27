package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Necroskitter
 * {1}{B}{B}
 * Creature — Elemental
 * 1/4
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 * Whenever a creature an opponent controls with a -1/-1 counter on it dies, you may return that
 * card to the battlefield under your control.
 */
val Necroskitter = card("Necroskitter") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elemental"
    power = 1
    toughness = 4
    oracleText = "Wither (This deals damage to creatures in the form of -1/-1 counters.)\n" +
        "Whenever a creature an opponent controls with a -1/-1 counter on it dies, you may return " +
        "that card to the battlefield under your control."

    keywords(Keyword.WITHER)

    // The dying creature's controller and counters are read from last-known information. The
    // return is guarded on the card still being in a graveyard (whosever it is), so a token or a
    // card that already left does nothing.
    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.opponentControls().withCounter(CounterType.MINUS_ONE_MINUS_ONE)
        ).dies()
        effect = Effects.May(
            Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.TriggeringEntity, underYourControl = true)
        )
        description = "Whenever a creature an opponent controls with a -1/-1 counter on it dies, you " +
            "may return that card to the battlefield under your control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02907b62-5456-4605-a8cd-321487928761.jpg?1783942686"
        ruling("2008-08-01", "It doesn't matter how many -1/-1 counters were on the creature when it was put into a graveyard from the battlefield, as long as it had at least one.")
        ruling("2008-08-01", "The card returns to the battlefield without any -1/-1 counters on it (unless some other effect causes it to enter with some).")
        ruling("2008-08-01", "It doesn't matter whose graveyard the creature is put into. It matters only that an opponent controlled it when it left the battlefield.")
    }
}
