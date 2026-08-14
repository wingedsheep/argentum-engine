package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Thoughtcast — Mirrodin #54
 * {4}{U} · Sorcery
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * Draw two cards.
 *
 * The affinity payoff common: five mana on an empty board, one blue with four artifacts out.
 * Affinity shaves generic mana only, so the {U} is never reduced away — the floor is {U}, not free.
 *
 * Affinity is a cost reduction, not an alternative cost: mana value stays 5 in every zone
 * regardless of how cheaply the spell was actually cast.
 */
val Thoughtcast = card("Thoughtcast") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Draw two cards."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Greg Hildebrandt"
        flavorText = "Vedalken eyes don't see the beauty in things. They see only what those things can teach."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efb965a7-877a-4302-b507-25b0a9e32d9b.jpg?1783944550"
    }
}
