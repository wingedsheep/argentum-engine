package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Misty Mountains Raider
 * {4}{R}
 * Creature — Goblin Soldier
 * 4/4
 *
 * Whenever you attack, amass Goblins 2.
 *
 * "Whenever you attack" is the once-per-combat declare-attackers trigger
 * ([Triggers.YouAttack]), not a per-attacker one — it fires once no matter how many creatures
 * were declared, and it fires even when the Raider itself stays home.
 */
val MistyMountainsRaider = card("Misty Mountains Raider") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Soldier"
    oracleText = "Whenever you attack, amass Goblins 2. (Put two +1/+1 counters on an Army you " +
        "control. It's also a Goblin. If you don't control an Army, create a 0/0 black Goblin Army " +
        "creature token first.)"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = Effects.Amass(2, "Goblin")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "105"
        artist = "Tomas Duchek"
        flavorText = "The Wargs and the Goblins often helped one another in wicked deeds."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dff14cd-b60b-48f4-9d9f-c9019b55df4c.jpg?1785152178"
    }
}
