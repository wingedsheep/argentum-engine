package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Desperate Castaways
 * {1}{B}
 * Creature — Human Pirate
 * 2/3
 * This creature can't attack unless you control an artifact.
 *
 * Gadrak's attack restriction at a count of one: [CantAttackUnless] over
 * [Conditions.YouControl] with an artifact filter. The restriction is checked only as
 * attackers are declared, so losing the artifact afterwards doesn't remove it from combat.
 */
val DesperateCastaways = card("Desperate Castaways") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 3
    oracleText = "This creature can't attack unless you control an artifact."

    staticAbility {
        ability = CantAttackUnless(Conditions.YouControl(GameObjectFilter.Artifact))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98a92dde-7a84-40fb-a11e-54c7d3057126.jpg?1783935762"
        ruling(
            "2017-09-29",
            "Once Desperate Castaways has attacked, it will remain in combat even if you no " +
                "longer control an artifact."
        )
    }
}
