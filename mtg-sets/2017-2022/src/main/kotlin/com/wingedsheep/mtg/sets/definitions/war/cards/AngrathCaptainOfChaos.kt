package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Angrath, Captain of Chaos — War of the Spark #227 (canonical printing)
 * {2}{B/R}{B/R}
 * Legendary Planeswalker — Angrath
 * Loyalty 5
 *
 * Creatures you control have menace.
 * −2: Amass Zombies 2.
 */
val AngrathCaptainOfChaos = card("Angrath, Captain of Chaos") {
    manaCost = "{2}{B/R}{B/R}"
    colorIdentity = "BR"
    typeLine = "Legendary Planeswalker — Angrath"
    startingLoyalty = 5
    oracleText = "Creatures you control have menace.\n" +
        "−2: Amass Zombies 2. (Put two +1/+1 counters on an Army you control. It's also a Zombie. " +
        "If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    // Creatures you control have menace.
    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    // −2: Amass Zombies 2.
    loyaltyAbility(-2) {
        effect = Effects.Amass(2, "Zombie")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa3a4cb5-945e-4caf-8a18-1ef977879fe8.jpg?1783933380"
        ruling("2023-06-16", "To amass Zombies N, if you don't control an Army creature, create a 0/0 black Zombie Army creature token. Then you choose an Army creature you control and put N +1/+1 counters on it. If that Army isn't already a Zombie, it becomes a Zombie in addition to its other types.")
        ruling("2023-06-16", "In the rare case that you control multiple Army creatures (perhaps because you played a creature with changeling) while you amass Zombies, you choose which of your Army creatures to put the +1/+1 counters on. If that creature isn't a Zombie, it becomes a Zombie in addition to its other types.")
    }
}
