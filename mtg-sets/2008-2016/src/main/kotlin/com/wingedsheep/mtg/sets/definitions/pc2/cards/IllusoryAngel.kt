package com.wingedsheep.mtg.sets.definitions.pc2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Illusory Angel
 * {2}{U}
 * Creature — Angel Illusion
 * 4/4
 *
 * Cast this spell only if you've cast another spell this turn.
 * Flying
 *
 * The restriction is checked while the cast is being validated, before this spell is recorded
 * in the turn's cast history, so "at least one spell cast this turn" is exactly "another spell".
 * The cast history keeps every spell cast whether it resolved, was countered, or is still on the
 * stack (the ruling below). It only gates casting — other effects can still put it onto the
 * battlefield.
 */
val IllusoryAngel = card("Illusory Angel") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Angel Illusion"
    oracleText = "Cast this spell only if you've cast another spell this turn.\nFlying"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    spell {
        castOnlyIf(Conditions.YouCastSpellsThisTurn(atLeast = 1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Allen Williams"
        flavorText = "\"Yes, she is made of pure belief—but I hardly see why that makes her unique.\"\n—Reith, master illusionist"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f7c0cb1-1102-4125-b9cd-a9a0478e3cb2.jpg?1783940631"
        ruling("2014-07-18", "It doesn't matter whether the other spell resolved. It could have been countered or, if you've somehow cast Illusory Angel as though it had flash, it could still be on the stack.")
    }
}
