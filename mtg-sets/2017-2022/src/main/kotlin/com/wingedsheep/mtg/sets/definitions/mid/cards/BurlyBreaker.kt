package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Burly Breaker // Dire-Strain Demolisher (Innistrad: Midnight Hunt)
 * {3}{G}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Burly Breaker (6/5): Ward {1}. Daybound.
 * Back  — Dire-Strain Demolisher (8/7): Ward {3}. Nightbound.
 */
private val BurlyBreakerFront = card("Burly Breaker") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 6
    toughness = 5
    oracleText = "Ward {1} (Whenever this creature becomes the target of a spell or ability an opponent " +
        "controls, counter it unless that player pays {1}.)\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3849ad37-f80d-4ffc-9240-25a63326b3dd.jpg?1783925591"
    }
}

private val DireStrainDemolisher = card("Dire-Strain Demolisher") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 8
    toughness = 7
    oracleText = "Ward {3} (Whenever this creature becomes the target of a spell or ability an opponent " +
        "controls, counter it unless that player pays {3}.)\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{3}")))
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "174"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/back/3/8/3849ad37-f80d-4ffc-9240-25a63326b3dd.jpg?1783925591"
    }
}

val BurlyBreaker: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BurlyBreakerFront,
    backFace = DireStrainDemolisher,
)
