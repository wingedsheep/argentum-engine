package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Rite of Undoing — Fate Reforged #49 (canonical printing)
 * {4}{U} · Instant
 *
 * Delve
 * Return target nonland permanent you control and target nonland permanent you don't control to
 * their owners' hands.
 *
 * Two separate single-target requirements (as Peel from Reality): the slots carry different
 * filters and each is checked for legality on its own at resolution, so one illegal target still
 * lets the other bounce. "You don't control" is `Not(ControlledByYou)`, not "an opponent
 * controls" — a teammate's permanents qualify too (as Cyclonic Rift).
 */
val RiteOfUndoing = card("Rite of Undoing") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\n" +
        "Return target nonland permanent you control and target nonland permanent you don't control " +
        "to their owners' hands."

    keywords(Keyword.DELVE)

    spell {
        val yours = target(TargetFilter(GameObjectFilter.NonlandPermanent.youControl()))
        val notYours = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ReturnToHand(yours) then Effects.ReturnToHand(notYours)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20afe614-a367-461a-a67b-02d5ed634cc5.jpg?1783938705"
    }
}
