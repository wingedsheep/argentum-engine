package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.dsl.MiscPatterns

/**
 * Treetop Sentries
 * {3}{G}
 * Creature — Squirrel Archer
 * 2/4
 *
 * Reach
 * When this creature enters, you may forage. If you do, draw a card.
 * (To forage, exile three cards from your graveyard or sacrifice a Food.)
 */
val TreetopSentries = card("Treetop Sentries") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Squirrel Archer"
    power = 2
    toughness = 4
    oracleText = "Reach\nWhen this creature enters, you may forage. If you do, draw a card. (To forage, exile three cards from your graveyard or sacrifice a Food.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ReflexiveTriggerEffect(
            action = MiscPatterns.forage(),
            optional = true,
            reflexiveEffect = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "201"
        artist = "Iris Compiet"
        flavorText = "They don't waste arrows on warning shots."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e16d4d6e-1fe5-4ff6-9877-8c849a24f5e0.jpg?1721426978"
    }
}
