package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Punish the Enemy
 * {4}{R}
 * Instant
 * Punish the Enemy deals 3 damage to target player or planeswalker and 3 damage to target creature.
 */
val PunishTheEnemy = card("Punish the Enemy") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Punish the Enemy deals 3 damage to target player or planeswalker and 3 damage to target creature."

    spell {
        val victim = target(Targets.PlayerOrPlaneswalker)
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, victim) then Effects.DealDamage(3, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "35"
        artist = "Slawomir Maniak"
        flavorText = "\"When justice descends, the servant will burn with the master.\"\n—Aurelia"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4179a72b-8482-46ec-9815-f5d6d94b5aa5.jpg?1783940037"
        ruling("2013-04-15", "You must be able to target a player and a creature in order to cast Punish the Enemy.")
        ruling("2013-04-15", "If one of Punish the Enemy's targets is illegal when it tries to resolve, it will still deal damage to the remaining legal target.")
    }
}
