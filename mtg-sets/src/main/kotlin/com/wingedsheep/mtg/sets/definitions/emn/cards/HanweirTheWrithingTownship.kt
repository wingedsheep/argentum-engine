package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hanweir, the Writhing Township
 * Legendary Creature — Eldrazi Ooze
 * 7/4
 * Trample, haste
 * Whenever Hanweir attacks, create two 3/2 colorless Eldrazi Horror creature tokens that are
 * tapped and attacking.
 *
 * This is a meld result (Hanweir Garrison + Hanweir Battlements). Meld is out of scope, so Hanweir
 * is authored as a normal colorless legendary creature with its printed abilities. The
 * tapped-and-attacking tokens use [CreateTokenEffect] with `tapped`/`attacking`.
 * `meldResult = true` keeps it out of booster, draft and deckbuilding pools — it's only ever
 * created by melding the pair.
 */
val HanweirTheWrithingTownship = card("Hanweir, the Writhing Township") {
    manaCost = ""
    meldResult = true
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi Ooze"
    power = 7
    toughness = 4
    oracleText = "Trample, haste\n" +
        "Whenever Hanweir attacks, create two 3/2 colorless Eldrazi Horror creature tokens that " +
        "are tapped and attacking."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CreateTokenEffect(
            count = DynamicAmount.Fixed(2),
            power = 3,
            toughness = 2,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Horror"),
            tapped = true,
            attacking = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130b"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/671fe14d-0070-4bc7-8983-707b570f4492.jpg?1782711861"
    }
}
