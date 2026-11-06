package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Painful Lesson
 * {2}{B}
 * Sorcery
 * Target player draws two cards and loses 2 life.
 */
val PainfulLesson = card("Painful Lesson") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player draws two cards and loses 2 life."

    spell {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(2, player) then Effects.LoseLife(2, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Adrian Majkrzak"
        flavorText = "\"You wish to remember this lesson? I will write it down for you.\"\n—Senifet, vizier of Bontu"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/becbd481-311d-4f85-aacd-df1f7d7ef587.jpg?1783936501"
    }
}
