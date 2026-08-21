package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Healer's Flock — Modern Horizons 2 #16
 * {W}{W}{W} · Creature — Bird · 3 / 3
 *
 * Flying, lifelink
 *
 * A vanilla-plus body: both words are engine-live keywords, so the whole card is one
 * `keywords(...)` call. `CardBuilder.build()` derives the printed keyword line from the enum
 * entries, so no `keywordAbility` wrapper is needed for a simple keyword with no cost.
 */
val HealersFlock = card("Healer's Flock") {
    manaCost = "{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 3
    oracleText = "Flying, lifelink"

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Joe Slucher"
        flavorText = "The sight of a flock overhead is bittersweet. It means many are wounded, but it also means help is on the way."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b93b5429-8512-4ab6-9ecd-fa270e0144f3.jpg?1783926891"
    }
}
