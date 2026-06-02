package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter

/**
 * Collective Inferno
 * {3}{R}{R}
 * Enchantment
 *
 * Convoke
 * As this enchantment enters, choose a creature type.
 * Double all damage that sources you control of the chosen type would deal.
 */
val CollectiveInferno = card("Collective Inferno") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "As this enchantment enters, choose a creature type.\n" +
        "Double all damage that sources you control of the chosen type would deal."

    keywords(Keyword.CONVOKE)

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = SourceFilter.Matching(GameObjectFilter.Any.youControl().withChosenSubtype()),
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ec084cc-997d-4079-b445-8f701ec3c277.jpg?1767732728"
    }
}
