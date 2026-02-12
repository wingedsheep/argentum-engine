package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Dawning Purist
 * {2}{W}
 * Creature — Human Cleric
 * 2/2
 * Whenever Dawning Purist deals combat damage to a player, you may destroy target
 * enchantment that player controls.
 * Morph {1}{W}
 */
val DawningPurist = card("Dawning Purist") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever Dawning Purist deals combat damage to a player, you may destroy target enchantment that player controls.\nMorph {1}{W}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        target = TargetPermanent(filter = TargetFilter.Enchantment.opponentControls())
        effect = MayEffect(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true))
    }

    morph = "{1}{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Brian Snõddy"
        flavorText = "\"My faith in the Ancestor is stronger than your faith in false gods.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8cb25b0-e4c3-4a4e-b722-ea30e695f917.jpg?1562938552"
    }
}
