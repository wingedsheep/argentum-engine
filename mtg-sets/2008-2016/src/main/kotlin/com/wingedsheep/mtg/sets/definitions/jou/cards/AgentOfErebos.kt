package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Agent of Erebos
 * {3}{B}
 * Enchantment Creature — Zombie
 * 2/2
 *
 * Constellation — Whenever this creature or another enchantment you control enters, exile target
 * player's graveyard.
 *
 * Grim Guardian's constellation watcher carrying Bojuka Bog's graveyard-exile pipeline.
 */
val AgentOfErebos = card("Agent of Erebos") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, exile " +
        "target player's graveyard."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
            exile(graveyard, player.asPlayer)
        }
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "exile target player's graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Cyril Van Der Haegen"
        flavorText = "Erebos's minions hunt the Returned and warn those who consider the same folly."
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e07a595-5579-4d5a-b07c-84239d690459.jpg?1783939439"
        ruling(
            "2014-04-26",
            "The constellation ability is mandatory. If you are the only legal target (perhaps because your " +
                "opponent has hexproof), you must target yourself and exile all the cards from your graveyard."
        )
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
