package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Minas Tirith
 * Legendary Land
 *
 * Minas Tirith enters tapped unless you control a legendary creature.
 * {T}: Add {W}.
 * {1}{W}, {T}: Draw a card. Activate only if you attacked with two or more creatures this turn.
 */
val MinasTirith = card("Minas Tirith") {
    typeLine = "Legendary Land"
    colorIdentity = "W"
    oracleText = "Minas Tirith enters tapped unless you control a legendary creature.\n{T}: Add {W}.\n{1}{W}, {T}: Draw a card. Activate only if you attacked with two or more creatures this turn."

    replacementEffect(
        EntersTapped(
            unlessCondition = Exists(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Creature.legendary()
            )
        )
    )

    // {T}: Add {W}.
    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {1}{W}, {T}: Draw a card. Activate only if you attacked with two or more creatures this turn.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouAttackedWithCreaturesThisTurn(
                    filter = GameObjectFilter.Creature,
                    atLeast = 2
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Arthur Yuan"
        flavorText = "\"I would see the White Tree in flower again in the courts of the kings, and Minas Tirith in peace.\"\n—Faramir"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b38b6760-616f-4b11-8ce7-ac1223c7fd53.jpg?1686970358"
    }
}
