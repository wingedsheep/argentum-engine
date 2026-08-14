package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Ten Rings — Marvel Super Heroes #251
 * {8} · Legendary Artifact · Mythic
 *
 * Your maximum hand size is ten.
 * At the beginning of your end step, if you have fewer than ten cards in hand, draw cards equal
 * to the difference.
 *
 * Implementation notes:
 * - "Your maximum hand size is ten" is the [SetMaximumHandSize] static (Winter, Misanthropic
 *   Guide's primitive) scoped to [Player.You] with a fixed amount. It *sets* the maximum rather
 *   than adding to it, so it also caps you at ten if something else raised it.
 * - The end-step ability has an intervening "if" (CR 603.4): [Conditions.CardsInHandAtMost] with
 *   `9` is the literal reading of "fewer than ten cards in hand", and as a `triggerCondition` the
 *   engine checks it both when the ability would trigger and again on resolution — so drawing
 *   back up to ten in response correctly makes it do nothing.
 * - "Cards equal to the difference" is recomputed on resolution, not fixed at trigger time:
 *   `10 − (cards in your hand)`. The draw and the hand-size cap are independent, so if your
 *   maximum hand size has been lowered by another effect you still draw up to ten here and then
 *   discard down during the cleanup step.
 */
val TheTenRings = card("The Ten Rings") {
    manaCost = "{8}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Your maximum hand size is ten.\n" +
        "At the beginning of your end step, if you have fewer than ten cards in hand, draw cards " +
        "equal to the difference."

    // Your maximum hand size is ten.
    staticAbility {
        ability = SetMaximumHandSize(
            player = Player.You,
            amount = DynamicAmount.Fixed(10)
        )
    }

    // At the beginning of your end step, if you have fewer than ten cards in hand,
    // draw cards equal to the difference.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.CardsInHandAtMost(9)
        effect = Effects.DrawCards(
            DynamicAmount.Subtract(
                DynamicAmount.Fixed(10),
                DynamicAmount.Count(Player.You, Zone.HAND)
            )
        )
        description = "At the beginning of your end step, if you have fewer than ten cards in " +
            "hand, draw cards equal to the difference."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "251"
        artist = "Arthur Yuan"
        flavorText = "\"The rings act as if they have a will of their own.\"\n—Shang-Chi"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2332bc91-b0f2-4911-844d-d1cc915cd6c8.jpg?1783902890"
    }
}
