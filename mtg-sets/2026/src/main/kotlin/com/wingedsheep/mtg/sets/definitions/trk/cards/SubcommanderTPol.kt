package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Subcommander T'Pol
 * {2}{U}
 * Legendary Creature — Vulcan Scientist
 * 3/1
 * Flash
 * When Subcommander T'Pol enters, choose one —
 * • Counter target activated or triggered ability.
 * • Tap up to one target artifact or creature. Put a stun counter on it.
 *
 * The enters trigger is a [ModalEffect.chooseOne] whose modes each carry their own target, as on
 * Deceiver Exarch: mode one is Stifle's [Effects.CounterAbility] over
 * [TargetFilter.ActivatedOrTriggeredAbilityOnStack]; mode two is the "up to one" (optional) tap +
 * stun counter of Consider the Prime Directive, widened to artifacts or creatures.
 */
val SubcommanderTPol = card("Subcommander T'Pol") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Vulcan Scientist"
    power = 3
    toughness = 1
    oracleText = "Flash\n" +
        "When Subcommander T'Pol enters, choose one —\n" +
        "• Counter target activated or triggered ability.\n" +
        "• Tap up to one target artifact or creature. Put a stun counter on it. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Counter target activated or triggered ability") {
                target(TargetFilter.ActivatedOrTriggeredAbilityOnStack)
                effect = Effects.CounterAbility()
            },
            mode("Tap up to one target artifact or creature and put a stun counter on it") {
                val permanent = target(
                    TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature),
                    optional = true
                )
                effect = Effects.Tap(permanent) then
                    Effects.AddCounters(CounterType.STUN, 1, permanent)
            }
        )
        description = "When Subcommander T'Pol enters, choose one — counter target activated or " +
            "triggered ability; or tap up to one target artifact or creature and put a stun counter on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d90016d-c2cb-457b-9d97-c3b9411c46e3.jpg?1793013074"
    }
}
