package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity

/**
 * Kitchen Imp — Modern Horizons 2 #89
 * {3}{B} · Creature — Imp · 2 / 2
 *
 * Flying, haste
 * Madness {B} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)
 *
 * A vanilla evasive body whose whole point is the madness price: haste matters because madness
 * (CR 702.35) lets you cast the creature at instant speed off someone else's discard, so it can
 * attack the turn it lands even on a turn that isn't yours to start.
 *
 * `madness(...)` is the whole implementation of the last line — `CardBuilder.build()` derives the
 * printed `Keyword.MADNESS` from the keyword ability, so only flying and haste are declared here.
 */
val KitchenImp = card("Kitchen Imp") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Imp"
    power = 2
    toughness = 2
    oracleText = "Flying, haste\n" +
        "Madness {B} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)"

    keywords(Keyword.FLYING, Keyword.HASTE)

    madness("{B}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Evyn Fong"
        flavorText = "\"Order up! Ooze aspic and jellied anurid eyes!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/3/836ae711-e62f-49ec-850e-d25f6fd2a4d4.jpg?1783926859"
    }
}
