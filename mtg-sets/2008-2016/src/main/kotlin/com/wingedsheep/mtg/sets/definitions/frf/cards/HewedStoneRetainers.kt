package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hewed Stone Retainers
 * {3}
 * Artifact Creature — Golem
 * 4/4
 * Cast this spell only if you've cast another spell this turn.
 *
 * The restriction is checked while the cast is being validated, before this spell is recorded
 * in the turn's cast history, so "at least one spell cast this turn" is exactly "another spell".
 * The cast history keeps every spell cast whether it resolved, was countered, or is still on the
 * stack (the ruling below). It only gates casting — other effects can still put it onto the
 * battlefield.
 */
val HewedStoneRetainers = card("Hewed Stone Retainers") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    oracleText = "Cast this spell only if you've cast another spell this turn."
    power = 4
    toughness = 4

    spell {
        castOnlyIf(Conditions.YouCastSpellsThisTurn(atLeast = 1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "David Seguin"
        flavorText = "\"Their origins are shrouded in mystery. I believe they are the protectors of the lost secrets of our world.\"\n—Jilaya, Temur whisperer"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/250bc19c-b882-42ae-b516-fe876d0d047d.jpg?1783938672"
        ruling("2014-11-24", "It doesn't matter whether the other spell resolved. It could have been countered or, if you've somehow cast Hewed Stone Retainers as though it had flash, it could still be on the stack.")
    }
}
