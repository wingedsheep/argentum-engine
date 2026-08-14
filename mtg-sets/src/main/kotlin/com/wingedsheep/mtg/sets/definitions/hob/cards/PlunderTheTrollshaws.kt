package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Plunder the Trollshaws — The Hobbit #51
 * {1}{U} · Instant · Common
 *
 * Draw a card. If this spell was cast from a graveyard, draw two cards instead.
 * Flashback {3}{U}
 *
 * "Instead" replaces the whole draw, so this is one branch or the other — a [ConditionalEffect] with
 * an else branch (draw two / draw one), not "draw one, then draw one more". Flashback is the only
 * way this card is normally cast from a graveyard, but [Conditions.WasCastFromGraveyard] reads the
 * cast-time zone stamp rather than the flashback marker, so any other graveyard-cast permission
 * (Yawgmoth's Will and friends) also turns on the bonus, exactly as printed.
 */
val PlunderTheTrollshaws = card("Plunder the Trollshaws") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card. If this spell was cast from a graveyard, draw two cards instead.\n" +
        "Flashback {3}{U} (You may cast this card from your graveyard for its flashback cost. " +
        "Then exile it.)"

    spell {
        effect = ConditionalEffect(
            condition = Conditions.WasCastFromGraveyard,
            effect = Effects.DrawCards(2),
            elseEffect = Effects.DrawCards(1)
        )
    }

    keywordAbility(KeywordAbility.flashback("{3}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Alexander Mokhov"
        flavorText = "The swords caught their eyes particularly, because of their beautiful " +
            "scabbards and jeweled hilts."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afb73190-b9bd-4744-a011-a37cd9c0148d.jpg?1785496438"
    }
}
