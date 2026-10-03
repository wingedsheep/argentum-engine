package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetPermanentOrPlayer

/**
 * Etched Slith
 * {1}{B}
 * Artifact Creature — Phyrexian Slith
 * 1/1
 *
 * Menace
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it. When you do,
 * you may remove a counter from another target permanent or opponent.
 *
 * The +1/+1 counter is the mandatory action of a CR 603.12 reflexive trigger, so the removal is a
 * separate stack object whose target is chosen as it triggers. The "may" sits on the removal
 * itself — the target is chosen regardless. "Another" excludes the Slith (`TargetOther`), and the
 * player half is opponents only. Player counters (poison, energy, …) live in the same
 * `CountersComponent`, so `RemoveCounterOfAnyKind` serves either half of the target.
 */
val EtchedSlith = card("Etched Slith") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Phyrexian Slith"
    oracleText = "Menace\nWhenever this creature deals combat damage to a player, put a +1/+1 counter on it. " +
        "When you do, you may remove a counter from another target permanent or opponent."
    power = 1
    toughness = 1

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ReflexiveTrigger(
            action = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            optional = false,
            descriptionOverride = "Put a +1/+1 counter on this creature. When you do, you may remove " +
                "a counter from another target permanent or opponent."
        ) {
            val victim = target(
                TargetOther(
                    TargetPermanentOrPlayer(
                        opponentsOnly = true,
                        descriptionOverride = "target permanent or opponent"
                    )
                )
            )
            effect = Effects.May(Effects.RemoveCounterOfAnyKind(victim))
        }
        description = "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it. " +
            "When you do, you may remove a counter from another target permanent or opponent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Allen Williams"
        flavorText = "The inherently adaptive bodies of the slith made them coveted subjects for compleation."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/428e255d-bdda-4265-91cf-d02962e818e4.jpg?1783911281"
    }
}
