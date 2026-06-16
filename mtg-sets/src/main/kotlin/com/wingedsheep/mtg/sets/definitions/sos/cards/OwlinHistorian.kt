package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Owlin Historian — Secrets of Strixhaven #24
 * {2}{W} · Creature — Bird Cleric · 2/3
 *
 * Flying
 * When this creature enters, surveil 1. (Look at the top card of your library. You may put
 * it into your graveyard.)
 * Whenever one or more cards leave your graveyard, this creature gets +1/+1 until end of turn.
 *
 * The leave-graveyard trigger batches per event, so it grows the Historian once per batch of
 * cards that leave the graveyard (CR 603.3 — one trigger per simultaneous batch).
 */
val OwlinHistorian = card("Owlin Historian") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Cleric"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\n" +
        "Whenever one or more cards leave your graveyard, this creature gets +1/+1 until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.surveil(1)
    }

    triggeredAbility {
        trigger = Triggers.CardsLeaveYourGraveyard()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Matheus Graef"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fe99be0-e1ec-485e-82f8-02eba7b82441.jpg?1775937078"
    }
}
