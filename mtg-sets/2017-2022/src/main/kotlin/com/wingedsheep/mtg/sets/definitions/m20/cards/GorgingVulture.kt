package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gorging Vulture
 * {2}{B}
 * Creature — Bird
 * 2/2
 * Flying
 * When this creature enters, mill four cards. You gain 1 life for each creature card milled
 * this way.
 *
 * The life is gained as one event (a single GainLife of the counted amount), not one event per
 * creature card, so "whenever you gain life" triggers fire once.
 */
val GorgingVulture = card("Gorging Vulture") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, mill four cards. You gain 1 life for each creature card " +
        "milled this way. (To mill four cards, put the top four cards of your library into your graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val milled = mill(4)
            val milledCreatures = selectAll(from = milled, filter = GameObjectFilter.Creature)
            run(Effects.GainLife(DynamicAmounts.distinctEntitiesIn(milledCreatures)))
        }
        description = "When this creature enters, mill four cards. You gain 1 life for each creature card milled this way."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37cbe5f2-5b6b-41de-9586-c7cc83464cf2.jpg?1783932992"
    }
}
