package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Assaultron Dominator
 * {1}{R}
 * Artifact Creature — Robot
 * 2/2
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever an artifact creature you control attacks, you may pay {E}. If you do, put your choice
 * of a +1/+1, first strike, or trample counter on that creature.
 *
 * The attack trigger fires once per attacking artifact creature you control (ruling), and "that
 * creature" is the trigger's [EffectTarget.TriggeringEntity]. "You may pay {E}. If you do" is an
 * [Effects.MayPay] over an exact one-energy payment — only offered when you have the energy — and
 * the counter kind is a resolution-time [Effects.ChooseAction] made after paying.
 */
val AssaultronDominator = card("Assaultron Dominator") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Robot"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever an artifact creature you control attacks, you may pay {E}. If you do, put your " +
        "choice of a +1/+1, first strike, or trample counter on that creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.ArtifactCreature.youControl()).attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 1),
            then = Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        label = "+1/+1 counter",
                        effect = Effects.AddCounters(
                            CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity
                        ),
                    ),
                    EffectChoice(
                        label = "First strike counter",
                        effect = Effects.AddCounters(CounterType.FIRST_STRIKE, 1, EffectTarget.TriggeringEntity),
                    ),
                    EffectChoice(
                        label = "Trample counter",
                        effect = Effects.AddCounters(CounterType.TRAMPLE, 1, EffectTarget.TriggeringEntity),
                    ),
                )
            ),
        )
        description = "Whenever an artifact creature you control attacks, you may pay {E}. If you do, " +
            "put your choice of a +1/+1, first strike, or trample counter on that creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Viko Menezes"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4112fadb-da03-441a-beeb-1dc90425e3f3.jpg?1783912406"
        ruling(
            "2024-03-08",
            "If multiple artifact creatures you control attack, Assaultron Dominator's last ability " +
                "triggers for each of them."
        )
    }
}
