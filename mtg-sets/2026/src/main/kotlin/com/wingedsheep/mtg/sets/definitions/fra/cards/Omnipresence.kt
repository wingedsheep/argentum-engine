package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost

/**
 * Omnipresence — Reality Fracture #110.
 *
 * Omniscience with a dynamic cap: a controller-only, hand-only [MayCastWithoutPayingManaCost]
 * whose spell filter is "mana value ≤ the number of creatures you control". The cap is evaluated
 * against the card being cast each time the permission is checked (offered and validated), so it
 * tracks the board as creatures come and go. The free cast is its own action variant — the player
 * still chooses between it and paying normally — and an {X} spell cast this way has X = 0
 * (CR 107.3b), so its mana value in hand is what the cap compares against.
 */
val Omnipresence = card("Omnipresence") {
    manaCost = "{5}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "You may cast spells with mana value less than or equal to the number of creatures " +
        "you control from your hand without paying their mana costs."

    staticAbility {
        ability = MayCastWithoutPayingManaCost(
            controllerOnly = true,
            fromZones = setOf(Zone.HAND),
            spellFilter = GameObjectFilter.Any.manaValueAtMostDynamic(DynamicAmounts.creaturesYouControl()),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "110"
        artist = "Bryan Sola"
        flavorText = "Her choice made, Tam called upon the one being who could fix what Jace had done."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eaf9dc77-c83b-49cf-84be-6bd791cb925e.jpg?1789470856"
        inBooster = false
    }
}
