package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Mage-Ring Bully
 * {1}{R}
 * Creature — Human Warrior
 * 2/2
 *
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * This creature attacks each combat if able.
 */
val MageRingBully = card("Mage-Ring Bully") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "This creature attacks each combat if able."
    power = 2
    toughness = 2

    prowess()

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Karl Kopinski"
        flavorText = "\"Don't be rude, Beleren. We just want to enjoy the view with you.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a129f22-dd7e-4b2c-a514-a2ac55bb5661.jpg?1783938328"
        ruling(
            "2015-06-22",
            "If, during its controller's declare attackers step, Mage-Ring Bully is tapped or is affected by a " +
                "spell or ability that says it can't attack, then Mage-Ring Bully doesn't attack. If there's a " +
                "cost associated with having it attack, its controller isn't forced to pay that cost. If they " +
                "don't, Mage-Ring Bully doesn't have to attack.",
        )
    }
}
