package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * General Ferrous Rokiric — Modern Horizons 2 #198 (canonical printing)
 * {1}{R}{W} · Legendary Creature — Human Soldier · 3/1
 *
 * Hexproof from monocolored
 * Whenever you cast a multicolored spell, create a 4/4 red and white Golem artifact creature token.
 *
 * Hexproof from monocolored is Niv-Mizzet, Supreme's printed [KeywordAbility.Hexproof] over
 * [ProtectionScope.Monocolored]; the cast trigger is Pyroconvergence's
 * `Triggers.you.casts(GameObjectFilter.Multicolored)`.
 */
val GeneralFerrousRokiric = card("General Ferrous Rokiric") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 1
    oracleText = "Hexproof from monocolored\n" +
        "Whenever you cast a multicolored spell, create a 4/4 red and white Golem artifact creature token."

    keywordAbility(KeywordAbility.Hexproof(ProtectionScope.Monocolored))

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Multicolored)
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Golem"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/7/d/7ddb9e14-b299-4064-a41b-0fb6b078942e.jpg?1783926588",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Matt Stewart"
        flavorText = "\"The Boros Legion always stands for justice, but it never stands alone.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/0/600dca0f-5964-45e7-86df-f16a5b4a0106.jpg?1783926816"
    }
}
