package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Measure of a Man — Star Trek #23
 * {2}{W} · Enchantment — Saga
 *
 * I — Exile up to one target nontoken artifact or creature.
 * II — Investigate.
 * III — Return the exiled card to the battlefield under its owner's control. You gain 2 life.
 *
 * - Chapter I exiles with `linkToSource = true`; chapter III gathers over
 *   [CardSource.FromLinkedExile], so it returns exactly what chapter I exiled (every card, if the
 *   chapter was copied) and nothing when chapter I had no target. The return goes to the owner.
 * - The 2 life is gained regardless of whether anything came back.
 */
val TheMeasureOfAMan = card("The Measure of a Man") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Exile up to one target nontoken artifact or creature.\n" +
        "II — Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "III — Return the exiled card to the battlefield under its owner's control. You gain 2 life."

    sagaChapter(1) {
        val permanent = target(
            TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).nontoken()),
            optional = true,
        )
        effect = Effects.Move(permanent, Zone.EXILE, linkToSource = true)
    }

    sagaChapter(2) {
        effect = Effects.Investigate()
    }

    sagaChapter(3) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            move(exiled, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        } then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Lazare Gvimradze"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9e6b01d-7eb5-4632-9d51-0012b58006da.jpg?1793101356"
    }
}
