package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Splinter, Hamato Yoshi
 * {1}{B}
 * Legendary Creature — Mutant Ninja Rat
 * 1/3
 *
 * Sneak {B} (You may cast this spell for {B} if you also return an unblocked
 * attacker you control to hand during the declare blockers step. He enters
 * tapped and attacking.)
 * Menace
 * Other Ninjas you control get +1/+1.
 */
val SplinterHamatoYoshi = card("Splinter, Hamato Yoshi") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Mutant Ninja Rat"
    oracleText = "Sneak {B} (You may cast this spell for {B} if you also return an unblocked attacker you control to hand during the declare blockers step. He enters tapped and attacking.)\nMenace (This creature can't be blocked except by two or more creatures.)\nOther Ninjas you control get +1/+1."
    power = 1
    toughness = 3

    sneak("{B}")
    keywords(Keyword.MENACE)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Ninja").youControl(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "April Prime"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ea9d072-aa98-405e-a475-26f93cc37e53.jpg?1771586909"
    }
}
