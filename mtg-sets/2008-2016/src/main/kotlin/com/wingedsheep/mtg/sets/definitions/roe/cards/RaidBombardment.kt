package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Raid Bombardment
 * {2}{R}
 * Enchantment
 * Whenever a creature you control with power 2 or less attacks, this enchantment deals 1 damage to
 * the player or planeswalker that creature is attacking.
 *
 * One trigger per qualifying attacker; power is checked only as it triggers. The damage goes to
 * that creature's declared defender ([EffectTarget.AttackedBy]) — read from its last-known
 * information if it has left the battlefield, and nothing at all when it attacks a battle.
 */
val RaidBombardment = card("Raid Bombardment") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control with power 2 or less attacks, this enchantment deals 1 damage " +
        "to the player or planeswalker that creature is attacking."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.powerAtMost(2).youControl()).attacks()
        effect = Effects.DealDamage(1, EffectTarget.AttackedBy())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Matt Cavotta"
        flavorText = "Goblins in the first wave of a fire raid always bemoan the aim of everyone else."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c2d1a48-efde-4134-95f0-b23f6cf85259.jpg?1783941971"
    }
}
