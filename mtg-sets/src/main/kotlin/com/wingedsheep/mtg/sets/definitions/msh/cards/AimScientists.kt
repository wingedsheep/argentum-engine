package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * A.I.M. Scientists
 * {3}{U}
 * Creature — Human Scientist Villain
 * 3/3
 *
 * When this creature enters, it connives.
 * Basic landcycling {2}
 *
 * Connive (CR 702.166) is the standard draw-then-discard-then-conditional-counter recipe, so the
 * ETB is a plain [Effects.Connive] on the source itself. Basic landcycling is the typed-cycling
 * variant that searches for a basic land card ([KeywordAbility.basicLandcycling]).
 */
val AimScientists = card("A.I.M. Scientists") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Scientist Villain"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, it connives. (Draw a card, then discard a card. If you " +
        "discarded a nonland card, put a +1/+1 counter on this creature.)\n" +
        "Basic landcycling {2} ({2}, Discard this card: Search your library for a basic land card, " +
        "reveal it, put it into your hand, then shuffle.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Connive()
    }

    keywordAbility(KeywordAbility.basicLandcycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Bartek Fedyczak"
        flavorText = "Science demands sacrifice."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab96b656-100e-491a-a8c9-94dbb9482c4d.jpg?1783902962"
    }
}
