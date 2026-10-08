package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hanweir, the Writhing Township
 * Legendary Creature — Eldrazi Ooze
 * 7/4
 * Trample, haste
 * Whenever Hanweir attacks, create two 3/2 colorless Eldrazi Horror creature tokens that are
 * tapped and attacking.
 *
 * This is a meld result (Hanweir Garrison + Hanweir Battlements); Hanweir Battlements' activated
 * ability melds the pair into it (CR 701.42). The tapped-and-attacking tokens use
 * [CreateTokenEffect] with `tapped`/`attacking`. `meldOf` declares the pair the meld effect checks
 * and keeps Hanweir out of booster, draft and deckbuilding pools — it's only ever created by melding.
 */
val HanweirTheWrithingTownship = card("Hanweir, the Writhing Township") {
    manaCost = ""
    meldOf("Hanweir Garrison", "Hanweir Battlements")
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi Ooze"
    power = 7
    toughness = 4
    oracleText = "Trample, haste\n" +
        "Whenever Hanweir attacks, create two 3/2 colorless Eldrazi Horror creature tokens that " +
        "are tapped and attacking."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            count = 2,
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
