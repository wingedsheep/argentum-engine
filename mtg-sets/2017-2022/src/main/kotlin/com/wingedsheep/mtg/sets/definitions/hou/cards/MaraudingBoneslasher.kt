package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Marauding Boneslasher
 * {2}{B}
 * Creature — Zombie Minotaur
 * 3/3
 *
 * This creature can't block unless you control another Zombie.
 *
 * The bare noun "Zombie" means any Zombie *permanent* you control; "another" excludes the
 * Boneslasher itself (it is a Zombie too).
 */
val MaraudingBoneslasher = card("Marauding Boneslasher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Minotaur"
    power = 3
    toughness = 3
    oracleText = "This creature can't block unless you control another Zombie."

    staticAbility {
        ability = CantBlockUnless(
            Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Victor Adame Minguez"
        flavorText = "With the Hekma destroyed, the wandering dead that sought entrance to Naktamun had free rein to lay waste to the city."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddfb7f50-14b1-4c7d-b4c8-f50bc14f4fb7.jpg?1783936039"
    }
}
