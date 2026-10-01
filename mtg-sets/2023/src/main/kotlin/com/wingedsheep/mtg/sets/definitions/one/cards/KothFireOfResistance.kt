package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Koth, Fire of Resistance — Phyrexia: All Will Be One #138
 * {2}{R}{R} · Legendary Planeswalker — Koth · Starting loyalty 4
 *
 *  - **+2** is the reveal-to-hand basic-land search ([Patterns.Library.searchLibrary] with
 *    `reveal = true`), restricted to *basic* Mountains.
 *  - **−3** counts Mountains you control at resolution (Spitting Earth's amount).
 *  - **−7** is a triggered emblem ([Effects.CreateGlobalTriggeredAbility], the Chandra, Spark
 *    Hunter shape), so it outlives Koth. Any Mountain counts — not only basic ones.
 */
val KothFireOfResistance = card("Koth, Fire of Resistance") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Koth"
    startingLoyalty = 4
    oracleText = "+2: Search your library for a basic Mountain card, reveal it, put it into your " +
        "hand, then shuffle.\n" +
        "−3: Koth deals damage to target creature equal to the number of Mountains you control.\n" +
        "−7: You get an emblem with \"Whenever a Mountain you control enters, this emblem deals " +
        "4 damage to any target.\""

    // +2: Search your library for a basic Mountain card, reveal it, put it into your hand, then shuffle.
    loyaltyAbility(+2) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.MOUNTAIN),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        )
        description = "Search your library for a basic Mountain card, reveal it, put it into your " +
            "hand, then shuffle."
    }

    // −3: Koth deals damage to target creature equal to the number of Mountains you control.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN)).count(),
            creature
        )
        description = "Koth deals damage to target creature equal to the number of Mountains you control."
    }

    // −7: Emblem — "Whenever a Mountain you control enters, this emblem deals 4 damage to any target."
    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN).youControl()).enters()
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(4, anyTarget)
                description = "Whenever a Mountain you control enters, this emblem deals 4 damage " +
                    "to any target."
            },
            descriptionOverride = "Whenever a Mountain you control enters, this emblem deals 4 " +
                "damage to any target."
        )
        description = "You get an emblem with \"Whenever a Mountain you control enters, this " +
            "emblem deals 4 damage to any target.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Eric Wilkerson"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6528e012-4091-4722-b706-c51772676167.jpg?1783918027"

        ruling("2023-02-04", "Koth's emblem is colorless. Notably, this means that protection from red won't stop a permanent or player from being the target of the emblem's ability, nor will it prevent the damage being dealt. Replacement effects that care about damage from a red source don't apply to it.")
    }
}
