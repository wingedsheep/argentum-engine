package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity

/**
 * Swarm, Being of Bees — Marvel's Spider-Man #69
 * {2}{B} · Legendary Creature — Insect Villain · 2/2
 *
 * Flash
 * Flying
 * Mayhem {B}
 */
val SwarmBeingOfBees = card("Swarm, Being of Bees") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Insect Villain"
    power = 2
    toughness = 2
    oracleText = "Flash\nFlying\n" +
        "Mayhem {B} (You may cast this card from your graveyard for {B} if you discarded it this " +
        "turn. Timing rules still apply.)"

    keywords(Keyword.FLASH, Keyword.FLYING)
    mayhem("{B}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"Evil never dies. It just becomes bees.\"\n—Swarm, Fritz von Meyer"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb83d54e-6641-4929-99ad-c0ba5b610902.jpg?1783905340"
    }
}
