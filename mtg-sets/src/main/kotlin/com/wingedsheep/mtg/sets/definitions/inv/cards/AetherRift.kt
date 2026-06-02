package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Costs

/**
 * Aether Rift
 * {1}{R}{G}
 * Enchantment
 * At the beginning of your upkeep, discard a card at random. If you discard a creature card
 * this way, return it from your graveyard to the battlefield unless any player pays 5 life.
 *
 * Composed from the standard random-discard pipeline (Gather hand → Select random → MoveCollection
 * to graveyard, storing the discarded card as "discarded") gated by
 * [Conditions.CollectionContainsMatch] on the discarded card. The reanimation is wrapped in
 * [Effects.UnlessAnyPlayerPays]: the discarded creature returns to the battlefield from "discarded"
 * unless any player pays 5 life in APNAP order.
 *
 * Rulings (2009-10-01): the card is genuinely discarded first (discard triggers fire). Then, in
 * turn order, each player may pay 5 life; if none does, the creature is put onto the battlefield.
 */
val AetherRift = card("Aether Rift") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, discard a card at random. If you discard a " +
        "creature card this way, return it from your graveyard to the battlefield unless any " +
        "player pays 5 life."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Composite(
            listOf(
                // Discard a card at random, remembering it as "discarded".
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.You),
                    storeAs = "hand"
                ),
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.Random(DynamicAmount.Fixed(1)),
                    storeSelected = "discarded"
                ),
                MoveCollectionEffect(
                    from = "discarded",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                    moveType = MoveType.Discard
                ),
                // If the discarded card was a creature, return it unless any player pays 5 life.
                ConditionalEffect(
                    condition = Conditions.CollectionContainsMatch("discarded", GameObjectFilter.Creature),
                    effect = Effects.UnlessAnyPlayerPays(
                        cost = Costs.pay.PayLife(5),
                        effect = MoveCollectionEffect(
                            from = "discarded",
                            destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/692c186a-997c-4f7e-a339-bf84884e1019.jpg?1562916173"
    }
}
