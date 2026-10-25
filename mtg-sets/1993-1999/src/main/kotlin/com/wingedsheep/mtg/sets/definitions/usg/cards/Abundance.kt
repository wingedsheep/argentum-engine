package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.EffectChoice

/**
 * Abundance
 * {2}{G}{G}
 * Enchantment
 * If you would draw a card, you may instead choose land or nonland and reveal cards from the top of
 * your library until you reveal a card of the chosen kind. Put that card into your hand and put all
 * other cards revealed this way on the bottom of your library in any order.
 *
 * An optional [ReplaceDrawWith] (the Parallel Thoughts shape): the engine offers it before each
 * individual draw, so a multi-card draw is replaced one card at a time (ruling), and before the
 * empty-library check, so it can stand in for a draw from an empty library (ruling). The kind is a
 * [Effects.ChooseAction] between two [Patterns.Library.revealUntilMatchToHand] reveals; with no card
 * of the chosen kind left the whole library is revealed and goes back in the order you choose.
 * The card is put into hand, not drawn, so draw triggers don't fire (ruling).
 */
val Abundance = card("Abundance") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "If you would draw a card, you may instead choose land or nonland and reveal cards " +
        "from the top of your library until you reveal a card of the chosen kind. Put that card into " +
        "your hand and put all other cards revealed this way on the bottom of your library in any order."

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        label = "Land",
                        effect = Patterns.Library.revealUntilMatchToHand(
                            filter = GameObjectFilter.Land,
                            restOrder = CardOrder.ControllerChooses
                        )
                    ),
                    EffectChoice(
                        label = "Nonland",
                        effect = Patterns.Library.revealUntilMatchToHand(
                            filter = GameObjectFilter.Nonland,
                            restOrder = CardOrder.ControllerChooses
                        )
                    ),
                )
            ),
            optional = true
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6d051fa-bd3a-4c94-ae41-34d89a7bb77d.jpg?1783946322"
        ruling(
            "2007-07-15",
            "If your library is empty, Abundance can prevent you from losing the game for being unable " +
                "to draw a card. If an effect or turn-based action would cause you to draw a card, you can " +
                "replace that draw with Abundance's replacement effect."
        )
        ruling("2004-10-04", "This replacement effect replaces the draw, so nothing that triggers on a draw will trigger.")
        ruling(
            "2004-10-04",
            "If you use this on a multi-card draw, each replaced draw is handled separately. In a " +
                "multi-card draw you do not have to choose how many of those draws will be replaced before " +
                "you do any drawing or use of this card."
        )
        ruling(
            "2004-10-04",
            "If no card of the chosen type is found before your library empties, you don't get a card, " +
                "but you do get to order all the cards in your library any way you choose."
        )
    }
}
