package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Devoted Retainer
 * {W}
 * Creature — Human Samurai
 * 1/1
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 */
val DevotedRetainer = card("Devoted Retainer") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Samurai"
    power = 1
    toughness = 1
    oracleText = "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)"

    keywordAbility(KeywordAbility.bushido(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Greg Hildebrandt"
        flavorText = "Deep within Eiganjo Castle lay the Palace of Infinite Halls, a seemingly endless network of corridors once guarded by a seemingly endless legion of samurai."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc41d6d6-d7e5-4874-b6e2-fa4c72454f15.jpg?1783944342"
    }
}
