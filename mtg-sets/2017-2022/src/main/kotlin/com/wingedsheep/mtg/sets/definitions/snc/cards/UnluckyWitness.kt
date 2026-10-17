package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Unlucky Witness
 * {R}
 * Creature — Human Citizen
 * 1/1
 * When this creature dies, exile the top two cards of your library. Until your next end step,
 * you may play one of those cards.
 *
 * Impulse exile of two with a `maxPlays = 1` grant: playing either card (casting it, or playing it
 * as a land) spends the permission for the other. The window is [MayPlayExpiry.UntilNextEndStep],
 * keyed to the controller's own end step.
 */
val UnluckyWitness = card("Unlucky Witness") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Citizen"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, exile the top two cards of your library. " +
        "Until your next end step, you may play one of those cards."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(2)))
            move(exiled, CardDestination.ToZone(Zone.EXILE))
            run(Effects.GrantMayPlayFromExile(
                from = exiled,
                expiry = MayPlayExpiry.UntilNextEndStep,
                maxPlays = 1
            ))
        }
        description = "When this creature dies, exile the top two cards of your library. " +
            "Until your next end step, you may play one of those cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Tyler Walpole"
        flavorText = "The guard took a midnight shift to earn a promotion. Unfortunately, so did the assassin."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec6430c0-67c1-4793-b4be-8f47545c57d6.jpg?1783923110"
        ruling(
            "2022-04-29",
            "You must pay all costs and follow all normal timing rules for a card played this way. For example, " +
                "you may play a land card this way only during a main phase of your own turn when the stack is " +
                "empty, and only if you haven't yet played a land this turn."
        )
    }
}
