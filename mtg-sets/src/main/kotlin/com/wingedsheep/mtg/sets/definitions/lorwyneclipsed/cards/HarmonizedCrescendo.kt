package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Harmonized Crescendo
 * {4}{U}{U}
 * Instant
 *
 * Convoke
 * Choose a creature type. Draw a card for each permanent you control of that type.
 */
val HarmonizedCrescendo = card("Harmonized Crescendo") {
    manaCost = "{4}{U}{U}"
    typeLine = "Instant"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Choose a creature type. Draw a card for each permanent you control of that type."

    keywords(Keyword.CONVOKE)

    spell {
        effect = CompositeEffect(listOf(
            ChooseOptionEffect(
                optionType = OptionType.CREATURE_TYPE,
                storeAs = "chosenType"
            ),
            DrawCardsEffect(
                count = DynamicAmount.AggregateBattlefield(
                    player = Player.You,
                    filter = GameObjectFilter.Permanent.withSubtypeFromVariable("chosenType")
                )
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Tyler Walpole"
        flavorText = "A song can unite quicker than a sword can divide."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/2715e0c0-9913-4bea-9a42-ad1164f6130a.jpg?1767659551"
    }
}
