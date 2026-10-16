package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vault 87: Forced Evolution (PIP #122)
 * {3}{G}{U} · Enchantment — Saga
 *
 * I — Gain control of target non-Mutant creature for as long as you control this Saga.
 * II — Put a +1/+1 counter on target creature you control. It becomes a Mutant in addition to its
 *      other types.
 * III — Draw cards equal to the greatest power among Mutants you control.
 *
 * - Chapter I is Kellogg's [Duration.WhileYouControlSource] steal: it ends the moment the Saga leaves
 *   the battlefield (including its own sacrifice after III) or changes controller, and an effect
 *   created after the Saga is already gone ends at once — the ruling's "you won't gain control at
 *   all". "Non-Mutant" is checked only on targeting and resolution; the control effect doesn't end if
 *   the creature later becomes a Mutant (2024-03-08 rulings).
 * - Chapter II's Mutant type is permanent (no duration printed).
 * - Chapter III reads projected power on resolution (2024-03-08 ruling).
 */
val Vault87ForcedEvolution = card("Vault 87: Forced Evolution") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Gain control of target non-Mutant creature for as long as you control this Saga.\n" +
        "II — Put a +1/+1 counter on target creature you control. It becomes a Mutant in addition to " +
        "its other types.\n" +
        "III — Draw cards equal to the greatest power among Mutants you control."

    sagaChapter(1) {
        val creature = target(TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype.MUTANT)))
        effect = Effects.GainControl(creature, Duration.WhileYouControlSource("this Saga"))
    }

    sagaChapter(2) {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.AddCreatureType(Subtype.MUTANT.value, creature, Duration.Permanent)
    }

    sagaChapter(3) {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.withSubtype(Subtype.MUTANT)).maxPower()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33bf267b-f41c-447a-aa35-0c01aad8a489.jpg?1783912383"
        ruling("2024-03-08", "If Vault 87 leaves the battlefield before its first chapter ability resolves, you won't gain control of the target creature at all.")
        ruling("2024-03-08", "The target of Vault 87's first chapter ability only needs to be a non-Mutant when that ability is put onto the stack and when that ability resolves. The control effect won't end if it later becomes a Mutant (perhaps because of the effect of the second chapter ability).")
        ruling("2024-03-08", "Use the greatest power among Mutants you control as Vault 87's last ability resolves to determine how many cards to draw.")
    }
}
