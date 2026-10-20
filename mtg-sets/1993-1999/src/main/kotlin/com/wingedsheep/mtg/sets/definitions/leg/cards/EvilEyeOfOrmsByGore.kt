package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Evil Eye of Orms-by-Gore
 * {4}{B}
 * Creature — Eye
 * 3/6
 * Non-Eye creatures you control can't attack.
 * This creature can't be blocked except by Walls.
 */
val EvilEyeOfOrmsByGore = card("Evil Eye of Orms-by-Gore") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eye"
    power = 3
    toughness = 6
    oracleText = "Non-Eye creatures you control can't attack.\n" +
        "This creature can't be blocked except by Walls."

    staticAbility {
        ability = CantAttack(
            filter = GroupFilter(GameObjectFilter.Creature.youControl().notSubtype(Subtype("Eye")))
        )
    }

    staticAbility {
        ability = CantBeBlockedExceptBy(
            blockerFilter = GameObjectFilter.Creature.withSubtype(Subtype.WALL)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Jesper Myrfors"
        flavorText = "\"The highway of fear is the shortest route to defeat.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b060f747-f65c-4ee0-923a-76298cb51a03.jpg?1783948067"
    }
}
