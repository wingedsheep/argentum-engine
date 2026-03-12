package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Siege-Gang Commander
 * {3}{R}{R}
 * Creature — Goblin
 * 2/2
 * When Siege-Gang Commander enters the battlefield, create three 1/1 red Goblin creature tokens.
 * {1}{R}, Sacrifice a Goblin: Siege-Gang Commander deals 2 damage to any target.
 */
val SiegeGangCommander = card("Siege-Gang Commander") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create three 1/1 red Goblin creature tokens.\n{1}{R}, Sacrifice a Goblin: This creature deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            count = 3
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
        )
        val any = target("any", Targets.Any)
        effect = Effects.DealDamage(2, any)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Aaron Miller"
        flavorText = "\"Ready . . . uh . . . fire!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f59ff087-97e7-4946-871f-1833abb2558a.jpg?1562745696"
        ruling("2022-12-08", "You can sacrifice any Goblin you control to activate Siege-Gang Commander's activated ability, including Siege-Gang Commander itself.")
    }
}
