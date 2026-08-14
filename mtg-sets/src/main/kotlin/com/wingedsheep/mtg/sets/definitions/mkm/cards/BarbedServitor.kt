package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Barbed Servitor — Murders at Karlov Manor #77
 * {3}{B} · Artifact Creature — Construct · 1/1
 *
 * Indestructible
 * When this creature enters, suspect it.
 * Whenever this creature deals combat damage to a player, you draw a card and you lose 1 life.
 * Whenever this creature is dealt damage, target opponent loses that much life.
 *
 * An indestructible 1/1 that wants to be hit. Suspecting itself is what makes the last ability
 * live: menace pushes it through, "can't block" is no loss on a body this small, and anything the
 * opponent points at it converts straight into life loss for them.
 *
 * "That much life" reads the *damage event's* amount, not the Servitor's toughness — CR damage is
 * dealt in full even when it exceeds toughness, so a Lightning Bolt drains 3 off an indestructible
 * 1/1. That's [ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT] off [Triggers.TakesDamage], the same
 * idiom Innocent Bystander uses for its "3 or more damage" gate — and it fires once per damage
 * *event*, so two blockers dealing 2 each queue two separate triggers of 2.
 *
 * The dealt-damage ability targets, so it goes on the stack after the damage is already dealt.
 * Ordering matters for the printed ruling about simultaneous lethal damage: if the same combat
 * brings you to 0, you lose to the state-based check before this ever resolves. The engine's SBA
 * pass runs before triggers go on the stack, which is exactly that behaviour.
 */
val BarbedServitor = card("Barbed Servitor") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "Indestructible\n" +
        "When this creature enters, suspect it. (It has menace and can't block.)\n" +
        "Whenever this creature deals combat damage to a player, you draw a card and you lose 1 life.\n" +
        "Whenever this creature is dealt damage, target opponent loses that much life."

    keywords(Keyword.INDESTRUCTIBLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Suspect(EffectTarget.Self)
        description = "When this creature enters, suspect it."
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.LoseLife(1, EffectTarget.Controller),
        )
        description = "Whenever this creature deals combat damage to a player, you draw a card " +
            "and you lose 1 life."
    }

    triggeredAbility {
        trigger = Triggers.TakesDamage
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.LoseLife(
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            opponent,
        )
        description = "Whenever this creature is dealt damage, target opponent loses that much life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c34e4ae-9bf3-4098-88f1-267e7d6cfa35.jpg?1783912901"

        ruling(
            "2024-02-02",
            "A creature can be dealt an amount of damage greater than its toughness. For example, " +
                "if Barbed Servitor is dealt 3 damage, its last ability causes the target opponent " +
                "to lose 3 life."
        )
        ruling(
            "2024-02-02",
            "If your life total is brought to 0 or less at the same time that Barbed Servitor is " +
                "dealt damage, you lose the game before its last ability goes on the stack."
        )
        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
    }
}
