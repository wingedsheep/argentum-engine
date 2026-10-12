package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Rust Goliath
 * {10}
 * Artifact Creature — Construct
 * 10/10
 * Prototype {3}{G}{G} — 3/5
 * Reach, trample
 */
val RustGoliath = card("Rust Goliath") {
    manaCost = "{10}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Construct"
    power = 10
    toughness = 10
    oracleText = "Prototype {3}{G}{G} — 3/5 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Reach, trample"

    keywordAbility(KeywordAbility.prototype("{3}{G}{G}", 3, 5))
    keywords(Keyword.REACH, Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Nicholas Gregory"
        flavorText = "\"You know those seedpods cluttering the eastern approach? I've got an idea . . . .\"\n—Calle, Urzan sergeant"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8348f73d-4226-4296-9d51-a2c497ee8270.jpg?1783920035"
    }
}
