package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Pillar of War — Born of the Gods #160
 * {3} · Artifact Creature — Golem · 3/3
 *
 * Defender
 * As long as this creature is enchanted, it can attack as though it didn't have defender.
 */
val PillarOfWar = card("Pillar of War") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 3
    toughness = 3
    oracleText = "Defender\nAs long as this creature is enchanted, it can attack as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.SourceMatches(GameObjectFilter.Any.enchanted())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Aleksi Briclot"
        flavorText = "Just because a temple has no guards doesn't mean it's undefended."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4c1371b-ffec-41c7-8fc1-acb2bd538080.jpg?1783939521"
    }
}
