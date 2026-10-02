package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Frogmyr Enforcer
 * {7}
 * Artifact Creature — Frog Myr
 * 4/4
 * Prototype {3}{R} — 2/2
 * Affinity for artifacts
 */
val FrogmyrEnforcer = card("Frogmyr Enforcer") {
    manaCost = "{7}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Frog Myr"
    power = 4
    toughness = 4
    oracleText = "Prototype {3}{R} — 2/2 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)"

    keywordAbility(KeywordAbility.prototype("{3}{R}", 2, 2))
    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/981fdd21-a650-4360-9ea5-550399d1da91.jpg?1783911272"
    }
}
