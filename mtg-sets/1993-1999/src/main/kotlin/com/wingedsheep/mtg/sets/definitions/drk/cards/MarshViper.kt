package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marsh Viper
 * {3}{G}
 * Creature — Snake
 * 1/2
 * Whenever this creature deals damage to a player, that player gets two poison counters.
 *
 * Note the trigger is on *any* damage, not just combat damage — a Marsh Viper that pings a player
 * some other way still poisons them — so this takes `Triggers.dealsDamage`'s default
 * `DamageType.Any` rather than Fynn the Fangbearer's combat-only variant of the same clause.
 */
val MarshViper = card("Marsh Viper") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 1
    toughness = 2
    oracleText = "Whenever this creature deals damage to a player, that player gets two poison " +
        "counters. (A player with ten or more poison counters loses the game.)"

    triggeredAbility {
        trigger = Triggers.dealsDamage(recipient = RecipientFilter.AnyPlayer)
        effect = Effects.AddCounters(
            counterType = Counters.POISON,
            count = 2,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Ron Spencer"
        flavorText = "\"All we had left were their black and bloated bodies.\" " +
            "—Maeveen O'Donagh, *Memoirs of a Soldier*"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/109cce7a-96f7-4e67-878a-bd5c93ea8643.jpg?1783947931"
    }
}
