package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
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

    // Reflexive: removing a counter is the cost that arms the destroy — Rustmouth Ogre's
    // "that player controls" combat-damage target hung off a remove-a-counter action.
    //
    // "Remove a counter" is any kind, not just the hexproof one she enters with: once anything
    // has put +1/+1 counters on her (Ouroboroid's combat trigger, an Adapt) the controller
    // chooses which kind to take off, and hardcoding `Counters.HEXPROOF` silently spent her
    // hexproof every time. [Effects.RemoveCounterOfAnyKind] is the choice-carrying primitive — it
    // prompts per counter kind present for a total of exactly one. Not `RemoveCountersUpTo(1, …)`:
    // a bare ceiling lets the controller say yes and then answer 0 to every prompt, which under
    // CR 603.12 would fire "when you do" without the "you do" ever happening.
    //
    // "That player controls" is the player Leatherhead just damaged, so the target filter is
    // `controlledByTriggeringPlayer()` rather than "any opponent" — the two only coincide in a
    // two-player game.
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = ReflexiveTriggerEffect(
            action = Effects.RemoveCounterOfAnyKind(EffectTarget.Self),
            optional = true,
            reflexiveEffect = Effects.Destroy(EffectTarget.ContextTarget(0)),
            reflexiveTargetRequirements = listOf(
                TargetPermanent(
                    filter = TargetFilter(
                        GameObjectFilter.ArtifactOrEnchantment.controlledByTriggeringPlayer()
                    )
                )
            ),
            // The yes/no prompt should read as the card does, not as the composed
            // "remove up to 1 counter" primitive's own wording.
            descriptionOverride = "You may remove a counter from Leatherhead. When you do, " +
                "destroy target artifact or enchantment that player controls."
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
