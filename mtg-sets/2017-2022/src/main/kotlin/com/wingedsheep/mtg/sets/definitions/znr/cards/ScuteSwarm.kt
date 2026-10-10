package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Scute Swarm
 * {2}{G}
 * Creature — Insect
 * 1/1
 * Landfall — Whenever a land you control enters, create a 1/1 green Insect creature token. If you
 * control six or more lands, create a token that's a copy of this creature instead.
 *
 * The land count is checked on resolution ("instead" picks one branch then).
 */
val ScuteSwarm = card("Scute Swarm") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 1
    toughness = 1
    oracleText = "Landfall — Whenever a land you control enters, create a 1/1 green Insect creature " +
        "token. If you control six or more lands, create a token that's a copy of this creature instead."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.If(
            Conditions.ControlLandsAtLeast(6),
            then = Effects.CreateTokenCopyOfSelf(),
            otherwise = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Insect"),
                imageUri = "https://cards.scryfall.io/normal/front/8/4/84da9c36-5d9c-4e29-b6cc-c5c10e490f2e.jpg?1783929499",
            ),
        )
        description = "Landfall — Whenever a land you control enters, create a 1/1 green Insect " +
            "creature token. If you control six or more lands, create a token that's a copy of this " +
            "creature instead."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Alex Konstad"
        flavorText = "\"Survival rule 782: There are *always* more scute bugs.\"\n—Zurdi, goblin shortcutter"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61f42823-8a48-4b81-a037-664ba1c69f29.jpg?1783929333"

        ruling(
            "2020-09-25",
            "The token copy will have Scute Swarm's ability. It will also be able to create copies of itself."
        )
        ruling(
            "2020-09-25",
            "The token copy won't copy counters or damage marked on Scute Swarm, nor will it copy other " +
                "effects that have changed Scute Swarm's power, toughness, types, color, and so on. " +
                "Normally, this means the token will simply be a Scute Swarm, but if any copy effects " +
                "have affected the original Scute Swarm, the token will take those into account."
        )
        ruling(
            "2020-09-25",
            "If Scute Swarm leaves the battlefield before its triggered ability resolves, the token will " +
                "still enter the battlefield as a copy of Scute Swarm, using Scute Swarm's copiable values " +
                "from when it was last on the battlefield."
        )
    }
}
