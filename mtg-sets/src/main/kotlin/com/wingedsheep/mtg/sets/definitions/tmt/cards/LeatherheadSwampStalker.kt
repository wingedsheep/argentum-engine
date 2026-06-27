package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Leatherhead, Swamp Stalker
 * {2}{G}{G}
 * Legendary Creature — Crocodile Mutant Rogue
 * 5/4
 *
 * Trample
 * Leatherhead enters with a hexproof counter on her.
 * Whenever Leatherhead deals combat damage to a player, you may remove a counter
 * from her. When you do, destroy target artifact or enchantment that player controls.
 */
val LeatherheadSwampStalker = card("Leatherhead, Swamp Stalker") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Crocodile Mutant Rogue"
    oracleText = "Trample\nLeatherhead enters with a hexproof counter on her.\nWhenever Leatherhead deals combat damage to a player, you may remove a counter from her. When you do, destroy target artifact or enchantment that player controls."
    power = 5
    toughness = 4

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.HEXPROOF),
            count = 1,
            selfOnly = true
        )
    )

    // Reflexive: removing a counter (the only kind she carries is hexproof) is the cost that
    // arms the destroy — modelled like Slumbering Walker (remove-a-counter reflexive) with
    // Dawning Purist's "that player controls" combat-damage target.
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = ReflexiveTriggerEffect(
            action = Effects.RemoveCounters(Counters.HEXPROOF, 1, EffectTarget.Self),
            optional = true,
            reflexiveEffect = Effects.Destroy(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetPermanent(filter = TargetFilter.ArtifactOrEnchantment.opponentControls())
            )
        )
        description = "Whenever Leatherhead deals combat damage to a player, you may remove a counter from her. When you do, destroy target artifact or enchantment that player controls."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1f6b5b5-12ca-468d-bc53-dd0cde60e7b6.jpg?1769006176"
    }
}
