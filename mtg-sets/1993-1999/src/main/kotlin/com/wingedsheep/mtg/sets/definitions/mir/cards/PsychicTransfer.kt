package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

val PsychicTransfer = card("Psychic Transfer") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "If the difference between your life total and target player's life total is 5 or less, exchange life totals with that player."

    spell {
        val player = target(Targets.Player)
        val difference = DynamicAmounts.yourLifeTotal() - DynamicAmounts.lifeTotal(player.asPlayer)
        // The absolute difference is checked on resolution, not as a targeting restriction.
        effect = Effects.If(
            condition = Conditions.All(
                Conditions.CompareAmounts(difference, ComparisonOperator.GTE, -5),
                Conditions.CompareAmounts(difference, ComparisonOperator.LTE, 5),
            ),
            then = Effects.ExchangeLifeTotals(player),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Dom!"
        flavorText = "\"The memory of your existence will fade like the final stars of morning.\"\n—Kaervek"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5507c474-5c4b-4292-b7bc-3ab4b48ea290.jpg?1783947104"
        ruling("2011-01-01", "If an effect says that a player can’t lose life, that player can’t exchange life totals with a player who has a lower life total; in that case, the exchange won’t happen.")
        ruling("2008-08-01", "If one of the players has a negative life total, you use their actual life total for determining the difference. If that difference is five or less, then the other player receives the negative life total.")
    }
}
