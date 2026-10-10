package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination

/**
 * Kinnan, Bonder Prodigy — Ikoria: Lair of Behemoths #192 (canonical printing)
 * {G}{U}
 * Legendary Creature — Human Druid
 * 2/2
 *
 * Whenever you tap a nonland permanent for mana, add one mana of any type that permanent produced.
 * {5}{G}{U}: Look at the top five cards of your library. You may put a non-Human creature card from
 * among them onto the battlefield. Put the rest on the bottom of your library in a random order.
 *
 * The first ability is the [AdditionalManaOnSourceTap] mirror (`color = null` copies the produced
 * type, colorless included) over nonland permanents you control — Roxanne, Starfall Savant's shape
 * with a wider filter. The second is [Patterns.Library.lookAtTopAndTakeMatching] with a battlefield
 * destination; the rest-on-the-bottom-in-random-order half is the recipe's default.
 */
val KinnanBonderProdigy = card("Kinnan, Bonder Prodigy") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Druid"
    power = 2
    toughness = 2
    oracleText = "Whenever you tap a nonland permanent for mana, add one mana of any type that " +
        "permanent produced.\n" +
        "{5}{G}{U}: Look at the top five cards of your library. You may put a non-Human creature card " +
        "from among them onto the battlefield. Put the rest on the bottom of your library in a random order."

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.NonlandPermanent.youControl(),
            color = null,
        )
    }

    activatedAbility {
        cost = Costs.Mana("{5}{G}{U}")
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Creature.notSubtype(Subtype.HUMAN),
            prompt = "You may put a non-Human creature card from among them onto the battlefield",
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "192"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63cda4a0-0dff-4edb-ae67-a2b7e2971350.jpg?1783931023"

        ruling("2020-04-17", "You're \"tapping a permanent for mana\" only if you're activating a mana ability of that permanent that includes the {T} symbol in its cost. A mana ability produces mana as part of its effect.")
        ruling("2020-04-17", "The types of mana are white, blue, black, red, green, and colorless.")
        ruling("2020-04-17", "The additional mana is produced by Kinnan, not by the nonland permanent that you tapped for mana.")
        ruling("2020-04-17", "Kinnan doesn't care about any restrictions or riders the nonland permanent put on the mana it produced. The additional mana Kinnan produces won't have any restrictions or riders.")
    }
}
