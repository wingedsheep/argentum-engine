package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Synapse Sliver
 * {4}{U}
 * Creature — Sliver
 * 3/3
 * Whenever a Sliver deals combat damage to a player, its controller may draw a card.
 */
val SynapseSliver = card("Synapse Sliver") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Sliver"
    power = 3
    toughness = 3
    oracleText = "Whenever a Sliver deals combat damage to a player, its controller may draw a card."

    val sliverFilter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))

    staticAbility {
        ability = GrantTriggeredAbilityToCreatureGroup(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsCombatDamageToPlayer.event,
                binding = Triggers.DealsCombatDamageToPlayer.binding,
                effect = Effects.DrawCards(1),
                optional = true
            ),
            filter = sliverFilter
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Thomas M. Baxa"
        flavorText = "\"Species XR17 feeds upon the mental energies of its victims. This explains why the goblins remain unaffected.\" —Riptide Project researcher"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bf966ff-0fd0-404d-be91-5b0c21035d73.jpg?1562923281"
    }
}
