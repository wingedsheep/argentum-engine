package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Thalia, Heretic Cathar
 * {2}{W}
 * Legendary Creature — Human Soldier
 * 3/2
 *
 * First strike
 * Creatures and nonbasic lands your opponents control enter tapped.
 *
 * The last ability is a global [PermanentsEnterTapped] runtime replacement (the group counterpart
 * of the self-only `EntersTapped`), consulted from the battlefield whenever some *other* permanent
 * enters — so `appliesTo` describes the *affected* permanents. Two separate replacements rather
 * than one union filter, because "creature" and "nonbasic land" are independent characteristic
 * checks: a nonbasic land that's also a creature (Dryad Arbor, an animated land) matches either
 * one and still just enters tapped.
 *
 * The controller-relative `opponentControls()` predicate resolves against Thalia's own controller
 * at entry time. Two consequences fall out for free and match the rulings:
 * - Permanents entering *simultaneously* with Thalia are unaffected — the replacement only exists
 *   once she is on the battlefield.
 * - A conditional "enters tapped unless …" replacement on the affected permanent (Port Town) does
 *   not save it: this effect taps it regardless, and per CR 614 only an `EntersUntapped` effect
 *   could override the tap.
 */
val ThaliaHereticCathar = card("Thalia, Heretic Cathar") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "First strike\n" +
        "Creatures and nonbasic lands your opponents control enter tapped."

    keywords(Keyword.FIRST_STRIKE)

    // Creatures your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    // Nonbasic lands your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.NonbasicLand.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Magali Villeneuve"
        flavorText = "\"Salvation will not be granted by the Lunarch Council. It must be earned—" +
            "at the edge of a sword, if necessary.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab0cee38-5e24-49d0-870c-22843ed4e101.jpg?1783937507"
        ruling(
            "2016-07-13",
            "If an effect states that a creature or land enters the battlefield tapped unless a " +
                "condition is met, Thalia's last ability has it enter tapped even if that condition " +
                "is true."
        )
        ruling(
            "2016-07-13",
            "If Thalia enters the battlefield at the same time as an opponent's creatures or " +
                "nonbasic lands, those creatures and lands aren't affected by Thalia's last ability."
        )
    }
}
