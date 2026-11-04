package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Forerunner of the Empire
 * {3}{R}
 * Creature — Human Soldier
 * 1/3
 * When this creature enters, you may search your library for a Dinosaur card, reveal it, then
 * shuffle and put that card on top.
 * Whenever a Dinosaur you control enters, you may have this creature deal 1 damage to each creature.
 *
 * See [ForerunnerOfTheCoalition] for why the search filter is [GameObjectFilter.Any]. The
 * damage hits every creature, Forerunner and the entering Dinosaur included.
 */
val ForerunnerOfTheEmpire = card("Forerunner of the Empire") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    oracleText = "When this creature enters, you may search your library for a Dinosaur card, " +
        "reveal it, then shuffle and put that card on top.\n" +
        "Whenever a Dinosaur you control enters, you may have this creature deal 1 damage to each creature."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype(Subtype.DINOSAUR),
            destination = SearchDestination.TOP_OF_LIBRARY,
            reveal = true
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.DINOSAUR).youControl()).enters()
        optional = true
        effect = Effects.ForEachInGroup(GroupFilter.AllCreatures, Effects.DealDamage(1, EffectTarget.IterationEntity))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/947644ab-02b3-4ebe-b62a-c087ab205ab0.jpg?1783935298"
        ruling(
            "2018-01-19",
            "If an effect refers to a \"[subtype] spell\" or \"[subtype] card,\" it refers only " +
                "to a spell or card that has that subtype. For example, March of the Drowned is " +
                "a card that benefits Pirates and features Pirates in its illustration, but it " +
                "isn't a Pirate card."
        )
    }
}
