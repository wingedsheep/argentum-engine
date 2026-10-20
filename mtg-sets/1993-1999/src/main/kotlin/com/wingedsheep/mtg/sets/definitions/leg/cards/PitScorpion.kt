package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pit Scorpion
 * {2}{B}
 * Creature — Scorpion
 * 1/1
 * Whenever this creature deals damage to a player, that player gets a poison counter.
 *
 * Marsh Viper's shape with one counter: the trigger fires on any damage to a player, not just
 * combat damage, and the damaged player receives the poison counter.
 */
val PitScorpion = card("Pit Scorpion") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Scorpion"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature deals damage to a player, that player gets a poison " +
        "counter. (A player with ten or more poison counters loses the game.)"

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(
            counterType = CounterType.POISON,
            count = 1,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Scott Kirschner"
        flavorText = "Sometimes the smallest nuisance can be the greatest pain."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc564f84-0d6e-4e09-a58d-a694d918cf12.jpg?1783948064"
    }
}
