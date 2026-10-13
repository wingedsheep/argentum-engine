package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nael, Avizoa Aeronaut
 * {2}{G}{U}
 * Legendary Creature — Elf Scout
 * 2/4
 * Flying
 * Domain — Whenever Nael deals combat damage to a player, look at the top X cards of your library,
 * where X is the number of basic land types among lands you control. Put up to one of them on top of
 * your library and the rest on the bottom in a random order. Then if there are five basic land types
 * among lands you control, draw a card.
 *
 * The look-and-sort is Thassa's Oracle's pipeline with domain as X. The "then if" is checked on
 * resolution, after the cards are put back, so a card kept on top is the one drawn.
 */
val NaelAvizoaAeronaut = card("Nael, Avizoa Aeronaut") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elf Scout"
    power = 2
    toughness = 4
    oracleText = "Flying\nDomain — Whenever Nael deals combat damage to a player, look at the top X " +
        "cards of your library, where X is the number of basic land types among lands you control. " +
        "Put up to one of them on top of your library and the rest on the bottom in a random order. " +
        "Then if there are five basic land types among lands you control, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val looked = gather(
                CardSource.TopOfLibrary(
                    count = DynamicAmounts.domain(),
                    player = Player.You
                )
            )
            val split = chooseUpToSplit(
                count = 1,
                from = looked,
                prompt = "Put up to one card on top of your library",
                selectedLabel = "Put on top of library",
                remainderLabel = "Put on the bottom in a random order"
            )
            toLibraryTop(split.selected)
            toLibraryBottom(split.remainder, order = CardOrder.Random)
        } then
            Effects.If(
                condition = Conditions.BasicLandTypesAtLeast(5),
                then = Effects.DrawCards(1)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Miguel Mercado"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5da59e8f-cbe5-4c14-a5a9-28d56048dc23.jpg?1783921281"
    }
}
