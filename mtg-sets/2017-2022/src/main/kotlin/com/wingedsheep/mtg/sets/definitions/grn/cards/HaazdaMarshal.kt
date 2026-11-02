package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Haazda Marshal
 * {W}
 * Creature — Human Soldier
 * 1/1
 * Whenever this creature and at least two other creatures attack, create a 1/1 white Soldier
 * creature token with lifelink.
 *
 * The battalion-shaped self attack trigger with `AttackerCountAtLeast(3)` (Makeshift Battalion).
 */
val HaazdaMarshal = card("Haazda Marshal") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature and at least two other creatures attack, create a 1/1 white " +
        "Soldier creature token with lifelink."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.LIFELINK)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Josh Hass"
        flavorText = "Ravnica's convoluted power structure often boils down to who can muster the most " +
            "swords on a given block."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c561d89-b02d-4f1a-b49a-cdf2def3404d.jpg?1783934201"
        ruling(
            "2018-10-05",
            "Once Haazda Marshal's ability has triggered, it doesn't matter how many creatures are still " +
                "attacking when that ability resolves.",
        )
    }
}
