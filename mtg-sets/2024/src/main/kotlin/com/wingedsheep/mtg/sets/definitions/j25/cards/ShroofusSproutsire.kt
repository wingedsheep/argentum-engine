package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Shroofus Sproutsire — Foundations Jumpstart #54 (canonical printing; earliest real printing)
 * {2}{G} · Legendary Creature — Saproling · 1/1 · Rare
 *
 * Trample
 * Whenever a Saproling you control deals combat damage to a player, create that many 1/1 green
 * Saproling creature tokens.
 *
 * The trigger is Sauron's subtype-filtered "a creature you control deals combat damage to a
 * player", which fires once per Saproling that connects. "That many" is that Saproling's own
 * damage, read off the triggering payload with [DynamicAmounts.triggerDamageAmount] — Living
 * Hive's and Lathril's shape. Shroofus is itself a Saproling, so its own hits count.
 */
val ShroofusSproutsire = card("Shroofus Sproutsire") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Saproling"
    power = 1
    toughness = 1
    oracleText = "Trample\n" +
        "Whenever a Saproling you control deals combat damage to a player, create that many 1/1 " +
        "green Saproling creature tokens."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withSubtype(Subtype.SAPROLING))
            .dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateToken(
            count = DynamicAmounts.triggerDamageAmount(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
        description = "Whenever a Saproling you control deals combat damage to a player, create " +
            "that many 1/1 green Saproling creature tokens."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Yukihiro Maruo"
        flavorText = "His laughter—and everything else about him—was infectious."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d29839f0-afe0-4caa-9628-655738aecce4.jpg?1783908855"
    }
}
