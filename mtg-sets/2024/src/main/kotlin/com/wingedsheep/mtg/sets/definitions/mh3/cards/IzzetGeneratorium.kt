package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Izzet Generatorium
 * {U}{R}
 * Artifact
 * If you would get one or more {E} (energy counters), you get that many plus one {E} instead.
 * {T}: Draw a card. Activate only if you've paid or lost four or more {E} this turn.
 */
val IzzetGeneratorium = card("Izzet Generatorium") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Artifact"
    oracleText = "If you would get one or more {E} (energy counters), you get that many plus one {E} instead.\n" +
        "{T}: Draw a card. Activate only if you've paid or lost four or more {E} this turn."

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.ENERGY,
                recipient = Recipient.Player(Player.You),
            ),
        )
    )

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.YouPaidOrLostEnergyThisTurn(4)))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Yeong-Hao Han"
        flavorText = "Hard hats are required. Shock-resistant coveralls aren't, but are highly recommended."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6d9537d-c6b9-46ef-834b-87750d79f1ae.jpg?1783911250"
    }
}
