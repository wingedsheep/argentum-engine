package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Virulent Silencer
 * {3}
 * Artifact Creature — Robot Assassin
 * 2/3
 * Whenever a nontoken artifact creature you control deals combat damage to a player,
 * that player gets two poison counters.
 */
val VirulentSilencer = card("Virulent Silencer") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Robot Assassin"
    power = 2
    toughness = 3
    oracleText = "Whenever a nontoken artifact creature you control deals combat damage to a player, " +
        "that player gets two poison counters. " +
        "(A player with ten or more poison counters loses the game.)"

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            sourceFilter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsCreature,
                    CardPredicate.IsArtifact,
                    CardPredicate.IsNontoken,
                ),
            ).youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.AddCounters(
            counterType = Counters.POISON,
            count = 2,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Kenn Yap"
        flavorText = "Nano-infectant: A category-Phi substance, capable of infecting both organics " +
            "and artificials. Weaponizing it is a war crime."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4120fcfc-3547-4774-a15d-b9cccac04e76.jpg?1752947571"
        ruling(
            "2025-07-25",
            "A player with ten or more poison counters loses the game. This is a state-based action " +
                "and doesn't use the stack. In other words, it happens immediately and players can't " +
                "respond to it, just like a player losing the game due to having 0 or less life.",
        )
    }
}
