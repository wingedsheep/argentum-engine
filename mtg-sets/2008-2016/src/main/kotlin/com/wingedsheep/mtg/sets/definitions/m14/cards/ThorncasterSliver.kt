package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thorncaster Sliver
 * {4}{R}
 * Creature — Sliver
 * 2/2
 * Sliver creatures you control have "Whenever this creature attacks, it deals 1 damage to any target."
 *
 * Hunter Sliver's granted-attack-trigger shape, restricted to Slivers you control like the other
 * Magic 2014 Slivers. The granted ability's source is the attacking Sliver, so that Sliver (not
 * Thorncaster) deals the damage.
 */
val ThorncasterSliver = card("Thorncaster Sliver") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Sliver"
    power = 2
    toughness = 2
    oracleText = "Sliver creatures you control have \"Whenever this creature attacks, it deals 1 damage to any target.\""

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(1, anyTarget)
                description = "Whenever this creature attacks, it deals 1 damage to any target."
            },
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SLIVER).youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Trevor Claxton"
        flavorText = "Every inch of a sliver's body is a potential claw, appendage, or projectile."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3655d837-945f-4ff5-8952-cff5f7b2d18f.jpg?1783939908"
        ruling("2013-07-01", "If you control multiple Thorncaster Slivers and a Sliver you control attacks, each ability will trigger separately.")
        ruling("2013-07-01", "If you change the creature type of a Sliver you control so it's no longer a Sliver, it will no longer be affected by its own ability. Its ability will continue to affect other Sliver creatures you control.")
    }
}
