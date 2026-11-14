package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bonded Construct — Magic Origins #223
 * {1} · Artifact Creature — Construct · 2/1
 *
 * This creature can't attack alone.
 *
 * "Can't attack alone" is the co-attacker restriction with the bare [GameObjectFilter.Creature]
 * filter: any other declared attacker satisfies it (as Raging Kronch).
 */
val BondedConstruct = card("Bonded Construct") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 1
    oracleText = "This creature can't attack alone."

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "223"
        artist = "Craig J Spearing"
        flavorText = "Loyal to a fault, it heeds its master's every command."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43361897-f853-4cab-a132-5af48dad3b54.jpg?1783938312"
        ruling("2015-06-22", "Bonded Construct can be declared as an attacker only if another creature is declared as an attacker at the same time.")
        ruling("2015-06-22", "If you control more than one creature that can't attack alone, they can attack together, even if no other creatures attack.")
        ruling("2015-06-22", "Although Bonded Construct can't attack alone, other attacking creatures don't have to attack the same player or planeswalker. For example, Bonded Construct could attack an opponent and another creature could attack a planeswalker that opponent controls.")
    }
}
