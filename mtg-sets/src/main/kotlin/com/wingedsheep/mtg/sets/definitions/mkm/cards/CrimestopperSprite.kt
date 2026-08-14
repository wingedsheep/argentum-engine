package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.collectEvidence
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Crimestopper Sprite
 * {2}{U}
 * Creature — Faerie Detective
 * 2/2
 *
 * As an additional cost to cast this spell, you may collect evidence 6.
 * Flying
 * When this creature enters, tap target creature. If evidence was collected, put a stun counter on it.
 *
 * The **rider** shape of the linkage, and the reason the intervening-if of [VituGhaziInspector] is
 * not the only pattern: here the trigger fires and targets *unconditionally* — the tap happens
 * whether or not evidence was collected — and only the extra clause is gated. So the condition sits
 * inside the effect as a [Conditions.WasEvidenceCollected]-gated second step, not on the trigger.
 *
 * `EffectTarget.ContextTarget(0)` for the stun counter is deliberate: "put a stun counter on **it**"
 * means the same creature that was tapped, so both steps read the one target slot.
 */
val CrimestopperSprite = card("Crimestopper Sprite") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Detective"
    power = 2
    toughness = 2
    oracleText = "As an additional cost to cast this spell, you may collect evidence 6. " +
        "(Exile cards with total mana value 6 or greater from your graveyard.)\n" +
        "Flying\n" +
        "When this creature enters, tap target creature. If evidence was collected, put a stun " +
        "counter on it. (If a permanent with a stun counter would become untapped, remove one " +
        "from it instead.)"

    collectEvidence(6)

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature", TargetCreature())
        effect = Effects.Composite(
            Effects.Tap(creature),
            ConditionalEffect(
                condition = Conditions.WasEvidenceCollected,
                effect = Effects.AddCounters(Counters.STUN, 1, creature),
            ),
        )
        description = "When this creature enters, tap target creature. If evidence was collected, " +
            "put a stun counter on it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc4ac597-38f0-48b5-ac2d-dfb0b169f834.jpg"
    }
}
