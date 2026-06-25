package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Living Phone
 * {2}{W}
 * Artifact Creature — Toy
 * 2/1
 *
 * When this creature dies, look at the top five cards of your library. You may reveal a creature
 * card with power 2 or less from among them and put it into your hand. Put the rest on the bottom
 * of your library in a random order.
 *
 * Dies-trigger flavor of the shared [Patterns.Library.lookAtTopRevealMatchingToHand] recipe: the
 * reveal is optional (choose up to one), filtered to creatures with power <= 2, and the rest go to
 * the bottom of the library in a random order (the pattern's defaults).
 */
val LivingPhone = card("Living Phone") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Toy"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, look at the top five cards of your library. You may " +
        "reveal a creature card with power 2 or less from among them and put it into your hand. " +
        "Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(5),
            filter = GameObjectFilter.Creature.powerAtMost(2),
            prompt = "You may reveal a creature card with power 2 or less to put into your hand",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Domenico Cava"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/8266f93b-f91c-4427-a222-075ceb0be3af.jpg?1726285933"
    }
}
