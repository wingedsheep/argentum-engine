package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Robobrain War Mind
 * {3}{U}
 * Artifact Creature — Robot
 * * /5
 *
 * Robobrain War Mind's power is equal to the number of cards in your hand.
 * When this creature enters, you get an amount of {E} (energy counters) equal to the number of
 * artifact creatures you control.
 * Whenever this creature attacks, you may pay {E}{E}{E}. If you do, draw a card.
 *
 * The starred power is a characteristic-defining `dynamicPower(...)` over hand size (Sylvan Yeti's
 * shape), so it applies in every zone. The energy count is read at resolution (ruling), including
 * Robobrain itself if it's still on the battlefield. "You may pay {E}{E}{E}. If you do" is an
 * [Effects.MayPay] over an exact three-energy payment (Assaultron Dominator's shape).
 */
val RobobrainWarMind = card("Robobrain War Mind") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot"
    toughness = 5
    oracleText = "Robobrain War Mind's power is equal to the number of cards in your hand.\n" +
        "When this creature enters, you get an amount of {E} (energy counters) equal to the number " +
        "of artifact creatures you control.\n" +
        "Whenever this creature attacks, you may pay {E}{E}{E}. If you do, draw a card."

    dynamicPower(DynamicAmounts.cardsInYourHand())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddDynamicCounters(
            CounterType.ENERGY,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactCreature).count(),
            EffectTarget.Controller,
        )
        description = "When this creature enters, you get an amount of {E} equal to the number of " +
            "artifact creatures you control."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 3),
            then = Effects.DrawCards(1),
        )
        description = "Whenever this creature attacks, you may pay {E}{E}{E}. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Leonardo Borazio"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19c0791d-f0c5-4673-a0c5-40540c1e6d8c.jpg?1783912411"
        ruling(
            "2024-03-08",
            "The ability that defines Robobrain War Mind's power functions in all zones, not just the battlefield."
        )
        ruling(
            "2024-03-08",
            "Use the number of artifact creatures you control when Robobrain War Mind's second ability " +
                "resolves, including Robobrain War Mind itself if it's still on the battlefield, to " +
                "determine the number of energy counters you get."
        )
    }
}
