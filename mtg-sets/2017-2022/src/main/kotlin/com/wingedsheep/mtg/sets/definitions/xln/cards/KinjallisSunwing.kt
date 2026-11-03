package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Kinjalli's Sunwing
 * {2}{W}
 * Creature — Dinosaur
 * 2/3
 * Flying
 * Creatures your opponents control enter tapped.
 *
 * The second ability is the group [PermanentsEnterTapped] replacement scoped to opponents'
 * creatures (as on Authority of the Consuls).
 */
val KinjallisSunwing = card("Kinjalli's Sunwing") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Creatures your opponents control enter tapped."

    keywords(Keyword.FLYING)

    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Simon Dominic"
        flavorText = "\"There are moments when I feel I could fly like the sunwing. Far away I would soar, " +
            "but always a golden gale pushes me back.\"\n—Huatli"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b9e0b0f-651a-44e6-8fb0-e46bfda0ada9.jpg?1783935798"
        ruling(
            "2017-09-29",
            "If a creature an opponent controls enters the battlefield at the same time that Kinjalli's " +
                "Sunwing enters the battlefield under your control, Kinjalli's Sunwing's effect doesn't " +
                "apply to your opponent's creature."
        )
    }
}
