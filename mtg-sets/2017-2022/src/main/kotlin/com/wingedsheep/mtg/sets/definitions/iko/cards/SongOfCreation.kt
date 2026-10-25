package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Song of Creation
 * {1}{G}{U}{R}
 * Enchantment
 * You may play an additional land on each of your turns.
 * Whenever you cast a spell, draw two cards.
 * At the beginning of your end step, discard your hand.
 */
val SongOfCreation = card("Song of Creation") {
    manaCost = "{1}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Enchantment"
    oracleText = "You may play an additional land on each of your turns.\n" +
        "Whenever you cast a spell, draw two cards.\n" +
        "At the beginning of your end step, discard your hand."

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 1)
    }

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.DrawCards(2)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Patterns.Hand.discardHand()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Noah Bradley"
        flavorText = "She wept, not from perfect understanding, but from serene gratitude."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7320233-1c83-4f61-8c08-cc8361867256.jpg?1783931016"
        ruling(
            "2020-04-17",
            "Song of Creation's first ability is cumulative if you control more than one. It's also " +
                "cumulative with other effects that let you play additional lands, such as the one from " +
                "Dryad of the Ilysian Grove."
        )
        ruling(
            "2020-04-17",
            "Song of Creation's second ability doesn't trigger when you cast it because it's not on the " +
                "battlefield yet."
        )
    }
}
