package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.collectEvidence
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Vitu-Ghazi Inspector
 * {1}{G}
 * Creature — Elf Detective
 * 1/3
 *
 * As an additional cost to cast this spell, you may collect evidence 6.
 * Reach
 * When this creature enters, if evidence was collected, put a +1/+1 counter on target creature and
 * you gain 2 life.
 *
 * The **enters-trigger** shape of the collect-evidence linkage (CR 701.59c, CR 607). The optional
 * cast cost rides the shared optional-additional-cost rail via `collectEvidence(6)`, stamping
 * `ChoiceSlot.EVIDENCE_COLLECTED` on the spell and then durably on the permanent it becomes — which
 * is what lets an enters-the-battlefield ability still read a fact decided while the card was on the
 * stack.
 *
 * The condition is an **intervening if**, so per CR 603.4 the ability doesn't go on the stack at all
 * when no evidence was collected, and therefore never asks for a target. Per CR 701.59b a caster
 * whose graveyard can't reach 6 is never offered the collect-evidence cast in the first place.
 */
val VituGhaziInspector = card("Vitu-Ghazi Inspector") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Detective"
    power = 1
    toughness = 3
    oracleText = "As an additional cost to cast this spell, you may collect evidence 6. " +
        "(Exile cards with total mana value 6 or greater from your graveyard.)\n" +
        "Reach\n" +
        "When this creature enters, if evidence was collected, put a +1/+1 counter on target " +
        "creature and you gain 2 life."

    collectEvidence(6)

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasEvidenceCollected
        val creature = target("target creature", TargetCreature())
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature),
            Effects.GainLife(2),
        )
        description = "When this creature enters, if evidence was collected, put a +1/+1 counter " +
            "on target creature and you gain 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/664d15d7-2724-4a9b-b5a7-8042d4b7da7b.jpg"
    }
}
