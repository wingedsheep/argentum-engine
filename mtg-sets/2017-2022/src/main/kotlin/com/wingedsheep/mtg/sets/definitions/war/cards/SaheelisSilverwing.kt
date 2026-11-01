package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Saheeli's Silverwing
 * {4}
 * Artifact Creature — Drake
 * 2/3
 * Flying
 * When this creature enters, look at the top card of target opponent's library.
 *
 * The look is a private gather of the targeted opponent's top card (Callous Deceiver's
 * "look at the top card" shape): the card is shown to the controller and stays where it is.
 */
val SaheelisSilverwing = card("Saheeli's Silverwing") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Drake"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, look at the top card of target opponent's library."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            gather(CardSource.TopOfLibrary(1, opponent.asPlayer))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Daniel Ljunggren"
        flavorText = "\"Track the source of those thopters,\" Saheeli told her creation. " +
            "\"And feel free to snack on as many as you want.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dba77c07-5d23-4a1a-90a0-b8293848d637.jpg?1783933370"
    }
}
