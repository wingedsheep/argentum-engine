package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.DynamicAmount.AggregateBattlefield

/**
 * Pillage the Bog
 * {B}{G}
 * Sorcery
 * Look at the top X cards of your library, where X is twice the number of lands you control.
 * Put one of them into your hand and the rest on the bottom of your library in a random order.
 * Plot {1}{B}{G}
 */
val PillageTheBog = card("Pillage the Bog") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Look at the top X cards of your library, where X is twice the number of lands you control. Put one of them into your hand and the rest on the bottom of your library in a random order.\nPlot {1}{B}{G} (You may pay {1}{B}{G} and exile this card from your hand. Cast it as a sorcery on a later turn without paying its mana cost. Plot only as a sorcery.)"

    spell {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmount.Multiply(
                AggregateBattlefield(Player.You, GameObjectFilter.Land),
                2
            ),
            keepCount = DynamicAmount.Fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
    }

    keywordAbility(KeywordAbility.plot("{1}{B}{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Forrest Imel"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa3b415f-7901-4ab4-84fe-60b90d40ac90.jpg?1712356177"
    }
}
