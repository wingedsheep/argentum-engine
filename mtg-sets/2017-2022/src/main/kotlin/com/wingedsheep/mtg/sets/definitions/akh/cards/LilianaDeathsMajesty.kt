package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Liliana, Death's Majesty — Amonkhet #97
 * {3}{B}{B} · Legendary Planeswalker — Liliana · Mythic · Starting loyalty 5
 *
 * +1: Create a 2/2 black Zombie creature token. Mill two cards.
 * −3: Return target creature card from your graveyard to the battlefield. That creature is a
 *     black Zombie in addition to its other colors and types.
 * −7: Destroy all non-Zombie creatures.
 *
 * The −3's rider has no duration, so (like Valkyrie's Call) it is an additive Layer 5 colour and
 * Layer 4 subtype grant with [Duration.Permanent] on the returned object — it lasts until that
 * creature leaves the battlefield. The −7 reads projected subtypes, so a creature made a Zombie by
 * the −3 survives it, and a Zombie with other types (a Zombie Jackal) is not "non-Zombie" (ruling).
 */
val LilianaDeathsMajesty = card("Liliana, Death's Majesty") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 5
    oracleText = "+1: Create a 2/2 black Zombie creature token. Mill two cards.\n" +
        "−3: Return target creature card from your graveyard to the battlefield. That creature is a " +
        "black Zombie in addition to its other colors and types.\n" +
        "−7: Destroy all non-Zombie creatures."

    // +1: Create a 2/2 black Zombie creature token. Mill two cards.
    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1783936411"
        ) then Patterns.Library.mill(2)
        description = "Create a 2/2 black Zombie creature token. Mill two cards."
    }

    // −3: Return target creature card from your graveyard to the battlefield. That creature is a
    //     black Zombie in addition to its other colors and types.
    loyaltyAbility(-3) {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD) then
            Effects.AddColor(Color.BLACK, creatureCard, Duration.Permanent) then
            Effects.AddCreatureType("Zombie", creatureCard, Duration.Permanent)
        description = "Return target creature card from your graveyard to the battlefield. That " +
            "creature is a black Zombie in addition to its other colors and types."
    }

    // −7: Destroy all non-Zombie creatures.
    loyaltyAbility(-7) {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.notSubtype(Subtype.ZOMBIE))
        description = "Destroy all non-Zombie creatures."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "97"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40d8f490-f04d-4d59-9ab0-a977527fd529.jpg?1783936503"
        ruling("2017-04-18", "A creature that is a Zombie and has other types, such as a Zombie Jackal, isn't a non-Zombie creature.")
    }
}
