package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Stensia Banquet
 * {2}{R}
 * Sorcery
 * Stensia Banquet deals damage to target opponent or planeswalker equal to the number of Vampires
 * you control.
 * Draw a card.
 *
 * The count is a board read at resolution (per the ruling), and "Vampires" counts every Vampire
 * permanent — a Kindred Vampire counts too — so the filter is a permanent subtype test rather than
 * a creature one. With no Vampires the spell still resolves for 0 damage and the draw.
 */
val StensiaBanquet = card("Stensia Banquet") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Stensia Banquet deals damage to target opponent or planeswalker equal to the " +
        "number of Vampires you control.\nDraw a card."

    spell {
        val victim = target(Targets.OpponentOrPlaneswalker)
        val vampires = DynamicAmounts
            .battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Vampire"))
            .count()
        effect = Effects.DealDamage(vampires, victim) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Greg Opalinski"
        flavorText = "\"We can't win a war on an empty stomach, can we?\"\n—Olivia Voldaren"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/828db19a-ca0d-4b82-a4b6-451125865093.jpg?1783937452"
        ruling(
            "2016-07-13",
            "You may cast Stensia Banquet targeting an opponent even if you control no Vampires. " +
                "You'll still draw a card.",
        )
        ruling("2016-07-13", "The number of Vampires you control is counted only as Stensia Banquet resolves.")
    }
}
