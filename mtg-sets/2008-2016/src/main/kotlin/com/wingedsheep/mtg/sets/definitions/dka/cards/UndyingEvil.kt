package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Undying Evil
 * {B}
 * Instant
 * Target creature gains undying until end of turn.
 */
val UndyingEvil = card("Undying Evil") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gains undying until end of turn. (When it dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.UNDYING, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Kev Walker"
        flavorText = "\"Is it true the evil that people do follows them into death? Let's find out.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/325f2243-54fd-484b-a742-166cea7ec179.jpg"
    }
}
