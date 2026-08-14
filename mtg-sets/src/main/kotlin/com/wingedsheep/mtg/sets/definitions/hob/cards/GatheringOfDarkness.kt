package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gathering of Darkness — The Hobbit #68
 * {3}{B} · Sorcery · Uncommon
 *
 * Return up to one target creature card from your graveyard to your hand.
 * Amass Goblins 3.
 *
 * Modeling notes:
 *  - "Up to one target" is an optional [TargetObject]: casting it with an empty graveyard (or
 *    declining the target) is legal, and the spell still amasses.
 *  - Both halves are one resolution, so the recursion happens before the amass — irrelevant here,
 *    but it is the printed order.
 */
val GatheringOfDarkness = card("Gathering of Darkness") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to one target creature card from your graveyard to your hand.\n" +
        "Amass Goblins 3. (Put three +1/+1 counters on an Army you control. It's also a Goblin. " +
        "If you don't control an Army, create a 0/0 black Goblin Army creature token first.)"

    spell {
        val creatureCard = target(
            "creature card in your graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD)
            )
        )

        effect = Effects.Move(creatureCard, Zone.HAND) then Effects.Amass(3, "Goblin")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Pavel Kolomeyets"
        flavorText = "\"Alas! Bolg of the North is coming!\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2ce066be-e5ad-4b93-8245-1b5018990d03.jpg?1784733910"
    }
}
