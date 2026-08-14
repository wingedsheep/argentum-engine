package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reckless Detective — Murders at Karlov Manor #141
 * {1}{R} · Creature — Devil Detective · 0/3
 *
 * Whenever this creature attacks, you may sacrifice an artifact or discard a card. If you do, draw
 * a card and this creature gets +2/+0 until end of turn.
 *
 * "**If** you do" — not "when you do" — so the payoff happens inside the same resolution, with no
 * second trip through the stack and nothing for an opponent to respond to in between. That rules
 * out `ReflexiveTriggerEffect` and leaves the K'un-Lun Warrior shape: a [MayEffect] over a
 * [ChooseActionEffect] whose two branches each pay their own cost and then run the payoff, so the
 * draw and the pump can only ever follow a cost that was actually paid.
 *
 * The [FeasibilityCheck]s do the "if you do" bookkeeping the branch structure can't: a controller
 * with no artifact never sees the sacrifice option, and one with an empty hand never sees the
 * discard option. With neither available the whole "may" is skipped rather than offered and then
 * silently no-opping into a free card.
 *
 * The pump is [EffectTarget.Self] and untargeted — the Detective doesn't target itself, so it still
 * gets +2/+0 even while an opponent's shroud effect or protection would block a targeted buff. A
 * 0/3 body that attacks for 2 only when it feeds itself is the whole design; the attack trigger
 * resolves before blockers are declared, so the +2/+0 is live for the block decision.
 */
val RecklessDetective = card("Reckless Detective") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil Detective"
    power = 0
    toughness = 3
    oracleText = "Whenever this creature attacks, you may sacrifice an artifact or discard a " +
        "card. If you do, draw a card and this creature gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        val payoff: Effect = Effects.DrawCards(1) then
            Effects.ModifyStats(2, 0, EffectTarget.Self)
        effect = MayEffect(
            effect = ChooseActionEffect(
                choices = listOf(
                    EffectChoice(
                        label = "Sacrifice an artifact",
                        effect = SacrificeEffect(filter = GameObjectFilter.Artifact) then payoff,
                        feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                            filter = GameObjectFilter.Artifact
                        ),
                    ),
                    EffectChoice(
                        label = "Discard a card",
                        effect = Patterns.Hand.discardCards(1) then payoff,
                        feasibilityCheck = FeasibilityCheck.HasCardsInZone(zone = Zone.HAND),
                    ),
                )
            ),
            descriptionOverride = "You may sacrifice an artifact or discard a card. If you do, " +
                "draw a card and this creature gets +2/+0 until end of turn.",
        )
        description = "Whenever this creature attacks, you may sacrifice an artifact or discard " +
            "a card. If you do, draw a card and this creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Tuan Duong Chu"
        flavorText = "He's gonna crack this thing wide open."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18da1a1d-e6ba-47e5-a545-0bacd427b782.jpg?1783912874"
    }
}
