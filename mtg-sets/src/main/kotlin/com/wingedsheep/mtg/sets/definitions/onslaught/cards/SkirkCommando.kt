package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Skirk Commando
 * {1}{R}{R}
 * Creature — Goblin
 * 2/1
 * Whenever Skirk Commando deals combat damage to a player, you may have it deal 2 damage
 * to target creature that player controls.
 * Morph {2}{R}
 */
val SkirkCommando = card("Skirk Commando") {
    manaCost = "{1}{R}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1
    oracleText = "Whenever Skirk Commando deals combat damage to a player, you may have it deal 2 damage to target creature that player controls.\nMorph {2}{R}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        val t = target("target", Targets.CreatureOpponentControls)
        effect = MayEffect(DealDamageEffect(2, t))
    }

    morph = "{2}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "228"
        artist = "Dave Dorman"
        flavorText = "Physical prowess and a complete lack of morals are the only requirements for the job."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c870a66-4cd5-4a8d-9948-feffa7d4ff11.jpg?1562928132"
    }
}
