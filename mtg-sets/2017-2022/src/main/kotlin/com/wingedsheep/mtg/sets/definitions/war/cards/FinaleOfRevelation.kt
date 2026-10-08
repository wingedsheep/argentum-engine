package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Finale of Revelation
 * {X}{U}{U}
 * Sorcery
 *
 * Draw X cards. If X is 10 or more, instead shuffle your graveyard into your library, draw X cards,
 * untap up to five lands, and you have no maximum hand size for the rest of the game.
 * Exile Finale of Revelation.
 */
val FinaleOfRevelation = card("Finale of Revelation") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw X cards. If X is 10 or more, instead shuffle your graveyard into your library, " +
        "draw X cards, untap up to five lands, and you have no maximum hand size for the rest of the game.\n" +
        "Exile Finale of Revelation."

    spell {
        selfExile()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.xValue(),
                ComparisonOperator.GTE,
                10,
            ),
            then = Effects.Pipeline {
                run(Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller))
                run(Effects.DrawCards(DynamicAmounts.xValue()))
                val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land))
                val landsToUntap = chooseUpTo(
                    5,
                    from = lands,
                    prompt = "Choose up to five lands to untap",
                    showAllCards = true
                )
                run(Effects.TapCollection(collection = landsToUntap, tap = false))
                run(Effects.RemoveMaximumHandSize())
            },
            otherwise = Effects.DrawCards(DynamicAmounts.xValue()),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "51"
        artist = "Johann Bodin"
        flavorText = "Ugin saw the gem that connected Bolas to his Meditation Realm as the key to his brother's downfall."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6630c34a-1a97-4e31-9d2c-1150b0aa903e.jpg?1783933465"
    }
}
