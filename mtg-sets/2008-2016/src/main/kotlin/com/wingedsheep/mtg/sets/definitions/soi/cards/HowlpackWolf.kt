package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Howlpack Wolf
 * {2}{R}
 * Creature — Wolf
 * 3/3
 *
 * This creature can't block unless you control another Wolf or Werewolf.
 *
 * "Wolf or Werewolf" means any permanent you control with either subtype; "another" excludes
 * the Howlpack Wolf itself (it is a Wolf too). Two Howlpack Wolves enable each other.
 */
val HowlpackWolf = card("Howlpack Wolf") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wolf"
    power = 3
    toughness = 3
    oracleText = "This creature can't block unless you control another Wolf or Werewolf."

    staticAbility {
        ability = CantBlockUnless(
            Conditions.YouControl(
                GameObjectFilter.Permanent.withAnySubtype("Wolf", "Werewolf"),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Svetlin Velinov"
        flavorText = "Wolves and werewolves join together for the common cause of the hunt."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7379689-8e4f-4c23-9276-3510f70ba7ff.jpg?1783937751"
        ruling("2016-04-08", "If you control a second Howlpack Wolf, they can both block.")
    }
}
