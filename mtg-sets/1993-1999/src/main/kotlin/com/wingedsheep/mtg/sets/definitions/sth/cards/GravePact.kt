package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grave Pact
 * {1}{B}{B}{B}
 * Enchantment
 * Whenever a creature you control dies, each other player sacrifices a creature of their choice.
 *
 * "Each other player" is [Player.EachOtherPlayer], not `EachOpponent`: in Two-Headed Giant the
 * ability makes your teammate sacrifice too (2023-09-01 ruling).
 */
val GravePact = card("Grave Pact") {
    manaCost = "{1}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control dies, each other player sacrifices a creature of their choice."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOtherPlayer))
        description = "Whenever a creature you control dies, each other player sacrifices a creature of their choice."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Scott Kirschner"
        flavorText = "\"The bonds of loyalty can tie one to the grave.\"\n—Crovax"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/940536be-fce1-4a90-9126-195815a43e0f.jpg?1783946563"
        ruling("2023-09-01", "When the triggered ability resolves, first the player whose turn it is (if it isn't your turn) chooses which creature they will sacrifice, then each other player in turn order does the same, then all chosen creatures are sacrificed at the same time.")
        ruling("2023-09-01", "None of the players are targeted. A player with hexproof would still sacrifice a creature.")
        ruling("2023-09-01", "If multiple creatures you control die at the same time, Grave Pact's ability will trigger that many times.")
        ruling("2023-09-01", "If you control more than one Grave Pact and a creature you control dies, each of the triggered abilities will trigger. Each other player will sacrifice a creature each time one of those abilities resolves.")
        ruling("2023-09-01", "If two players each control a Grave Pact and a creature controlled by one of those players dies, a chain reaction happens. First the ability of that player's Grave Pact will trigger, causing each other player to sacrifice a creature. That sacrifice causes the ability of the other player's Grave Pact to trigger, and so on.")
        ruling("2023-09-01", "In a Two-Headed Giant game, when a creature you control dies, Grave Pact's triggered ability will cause each other player, including your teammate, to sacrifice a creature. Grave Pact's ability won't trigger when a creature your teammate controls dies.")
    }
}
