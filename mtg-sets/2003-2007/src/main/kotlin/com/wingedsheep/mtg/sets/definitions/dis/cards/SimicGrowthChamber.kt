package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Simic Growth Chamber
 * Land
 *
 * This land enters tapped.
 * When this land enters, return a land you control to its owner's hand.
 * {T}: Add {G}{U}.
 *
 * "Return a land you control" doesn't target (CR 115.10a): the land is chosen as the
 * trigger resolves, so this gathers, chooses and moves rather than binding a target —
 * the same shape as Shrieking Drake's ETB. With no other land, this one is the only choice.
 */
val SimicGrowthChamber = card("Simic Growth Chamber") {
    typeLine = "Land"
    colorIdentity = "GU"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, return a land you control to its owner's hand.\n" +
        "{T}: Add {G}{U}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
            val returned = chooseExactly(
                1,
                from = lands,
                prompt = "Return a land you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN) then Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/407d0a0c-a6be-4bd5-8355-1715698c6bde.jpg?1783943376"
        ruling("2013-04-15", "If this land enters the battlefield and you control no other lands, its ability will force you to return it to your hand.")
    }
}
