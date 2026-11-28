package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Increasing Confusion — Dark Ascension #41 (canonical printing)
 * {X}{U} · Sorcery
 *
 * Target player mills X cards. If this spell was cast from a graveyard, that player mills twice
 * that many cards instead.
 * Flashback {X}{U}
 *
 * The flashback cost carries its own {X}, announced as the spell is cast from the graveyard.
 */
val IncreasingConfusion = card("Increasing Confusion") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player mills X cards. If this spell was cast from a graveyard, that player " +
        "mills twice that many cards instead.\n" +
        "Flashback {X}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val player = target(Targets.Player)
        effect = Effects.If(
            condition = Conditions.WasCastFromZone(Zone.GRAVEYARD),
            then = Patterns.Library.mill(DynamicAmounts.xValue() * 2, player),
            otherwise = Patterns.Library.mill(DynamicAmounts.xValue(), player),
        )
    }

    keywordAbility(KeywordAbility.flashback("{X}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13f5bcdc-70bb-4d67-99e1-282f166ee4bf.jpg?1783940839"
    }
}
