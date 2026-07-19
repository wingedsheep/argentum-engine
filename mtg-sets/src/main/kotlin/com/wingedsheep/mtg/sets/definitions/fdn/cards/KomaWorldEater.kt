package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Koma, World-Eater
 * {3}{G}{G}{U}{U}
 * Legendary Creature — Serpent
 * 8/12
 * This spell can't be countered.
 * Trample, ward {4}
 * Whenever Koma deals combat damage to a player, create four 3/3 blue Serpent creature tokens
 * named Koma's Coil.
 *
 * Pure composition of existing primitives:
 * - "can't be countered" → the card-level [cantBeCountered] flag.
 * - Trample → keyword; Ward {4} → [KeywordAbility.Ward] with a [WardCost.Mana] cost.
 * - The combat-damage trigger fires on [Triggers.DealsCombatDamageToPlayer] and creates four
 *   specifically-named ("Koma's Coil") 3/3 blue Serpent tokens via [CreateTokenEffect] — the raw
 *   effect is used directly because the `Effects.CreateToken` facade doesn't expose a token `name`,
 *   and Koma's Coil's name differs from its Serpent creature type.
 */
val KomaWorldEater = card("Koma, World-Eater") {
    manaCost = "{3}{G}{G}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Serpent"
    power = 8
    toughness = 12
    oracleText = "This spell can't be countered.\n" +
        "Trample, ward {4}\n" +
        "Whenever Koma deals combat damage to a player, create four 3/3 blue Serpent creature tokens named Koma's Coil."

    cantBeCountered = true
    keywords(Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{4}")))

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = CreateTokenEffect(
            count = 4,
            power = 3,
            toughness = 3,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Serpent"),
            name = "Koma's Coil",
            imageUri = "https://cards.scryfall.io/normal/front/c/e/cef25434-cd23-4dcf-b77e-36e648a8de23.jpg?1783908590",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Mark Zug"
        flavorText = "The gods imprisoned Koma, fearing he would eventually devour the Cosmos itself."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3b92caa-3401-4b11-9515-152f3e057c05.jpg?1783909091"
    }
}
