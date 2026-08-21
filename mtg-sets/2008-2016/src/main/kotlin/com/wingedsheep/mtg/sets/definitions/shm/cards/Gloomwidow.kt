package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gloomwidow
 * {2}{G}
 * Creature — Spider
 * 3 / 3
 *
 * Reach
 * This creature can block only creatures with flying.
 *
 * - Reach and the blocking restriction pull in opposite directions and both apply: reach lets it
 *   block fliers, and [CanOnlyBlockCreaturesWith] then forbids it from blocking anything else.
 * - The restriction is a static over projected state, so a creature that gains or loses flying
 *   after blockers would be declared is judged by its current keyword set, not its printed one.
 */
val Gloomwidow = card("Gloomwidow") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Spider"
    power = 3
    toughness = 3
    oracleText = "Reach\n" +
        "This creature can block only creatures with flying."

    keywords(Keyword.REACH)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Mark Tedin"
        flavorText = "When gloomwidows mature, they abandon venom in favor of massive webs that span the eaves of cliffs."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99bda306-1e37-4359-a649-fcd8a5a7e2fc.jpg?1783942742"
    }
}
