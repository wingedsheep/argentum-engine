package com.wingedsheep.mtg.sets.definitions.gpt.cards

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
 * Orzhov Basilica
 * Land
 *
 * This land enters tapped.
 * When this land enters, return a land you control to its owner's hand.
 * {T}: Add {W}{B}.
 *
 * "Return a land you control" doesn't target (CR 115.10a): the land is chosen as the
 * trigger resolves, so this gathers, chooses and moves rather than binding a target —
 * the same shape as Shrieking Drake's ETB. With no other land, this one is the only choice.
 */
val OrzhovBasilica = card("Orzhov Basilica") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, return a land you control to its owner's hand.\n" +
        "{T}: Add {W}{B}."

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
        effect = Effects.AddMana(Color.WHITE) then Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9154d2a-3fc5-4fd6-9885-a810cb6b542a.jpg?1783943456"
        ruling("2013-04-15", "If this land enters the battlefield and you control no other lands, its ability will force you to return it to your hand.")
    }
}
