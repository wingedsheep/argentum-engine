package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Filigree Attendant — Modern Horizons 2 #41
 * {2}{U}{U} · Artifact Creature — Homunculus · * / 3
 *
 * Flying
 * Filigree Attendant's power is equal to the number of artifacts you control.
 *
 * Cephalopod Sentry's shape exactly. The printed `*` power is a characteristic-defining ability
 * (CR 604.3), so no `power =` is set at all — [dynamicPower] over
 * [DynamicAmount.AggregateBattlefield] counting your artifacts *is* the power, recomputed in
 * layer 7a (CR 613.4a) and functioning in every zone, rather than a layer-7c (CR 613.4c) static
 * modification bolted onto a printed number.
 *
 * The Attendant is itself an artifact and so counts itself: on the battlefield it is never smaller
 * than 1/3.
 */
val FiligreeAttendant = card("Filigree Attendant") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Homunculus"
    // printed power is "*" — a characteristic-defining ability; see the Assay spec
    toughness = 3
    oracleText = "Flying\n" +
        "Filigree Attendant's power is equal to the number of artifacts you control."

    keywords(Keyword.FLYING)
    dynamicPower(DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Artifact))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Igor Kieryluk"
        flavorText = "\"The perfect assistant is one that's responsive, dexterous, and capable of reaching all the high shelves.\"\n—Coruk, Esper artificer"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0cb828c-a19b-4e6d-853f-85e5ea7cadf8.jpg?1783926880"
    }
}
