package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Drowsing Tyrannodon
 * {1}{G}
 * Creature — Dinosaur
 * 3/3
 * Defender
 * As long as you control a creature with power 4 or greater, this creature can attack
 * as though it didn't have defender.
 */
val DrowsingTyrannodon = card("Drowsing Tyrannodon") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 3
    oracleText = "Defender (This creature can't attack.)\nAs long as you control a creature with power 4 or greater, this creature can attack as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Simon Dominic"
        flavorText = "What the rangers thought were warning growls turned out to be nothing but monstrous snoring."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/288b056a-ea80-4fdc-990d-0ee1e9a7bf64.jpg?1783930678"

        ruling("2020-06-23", "Once Drowsing Tyrannodon has attacked, it will remain an attacking creature even if you no longer control a creature with power 4 or greater.")
        ruling("2020-06-23", "If Drowsing Tyrannodon's power is raised to 4 or greater, it will allow itself to attack.")
    }
}
