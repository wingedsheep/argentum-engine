package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Deep Space 9
 * {9}
 * Legendary Artifact — Spacecraft
 *
 * When Deep Space 9 enters and at the beginning of your upkeep, exile the top four cards of your
 * library. Put all permanent cards exiled this way onto the battlefield.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery.)
 * 9+ | Creatures you control get +3/+3.
 *
 * - No power/toughness box, so it never becomes a creature: its only station threshold grants a
 *   static, like The Eternity Elevator.
 * - "Enters and at the beginning of your upkeep" is two triggers sharing one effect.
 * - The exile is tracked (`moveTracked`) so only cards actually exiled this way are eligible; the
 *   permanent cards among them move together, so they enter simultaneously. Nonpermanent cards stay
 *   in exile.
 */
val DeepSpace9 = card("Deep Space 9") {
    manaCost = "{9}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Spacecraft"
    oracleText = "When Deep Space 9 enters and at the beginning of your upkeep, exile the top four " +
        "cards of your library. Put all permanent cards exiled this way onto the battlefield.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery.)\n" +
        "9+ | Creatures you control get +3/+3."

    fun exileTopFourDeployPermanents() = Effects.Pipeline {
        val top = gather(CardSource.TopOfLibrary(4))
        val exiled = moveTracked(top, CardDestination.ToZone(Zone.EXILE))
        move(
            exiled,
            CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
            filter = GameObjectFilter.Permanent
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = exileTopFourDeployPermanents()
        description = "When Deep Space 9 enters, exile the top four cards of your library. " +
            "Put all permanent cards exiled this way onto the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = exileTopFourDeployPermanents()
        description = "At the beginning of your upkeep, exile the top four cards of your library. " +
            "Put all permanent cards exiled this way onto the battlefield."
    }

    station()

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 9)
        ability = ModifyStats(3, 3, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "263"
        artist = "Paolo Puggioni"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76b039c5-e0a1-410a-954b-52f304e31f7f.jpg?1792508419"
    }
}
