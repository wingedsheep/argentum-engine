package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.ZoneChangeCause

/**
 * Abandoned Sarcophagus — Hour of Devastation #158 (canonical printing)
 * {3} · Artifact
 *
 * You may cast spells that have a cycling ability from your graveyard.
 * If a card that has a cycling ability would be put into your graveyard from anywhere and it
 * wasn't cycled, exile it instead.
 *
 * - The cast half is a [MayCastFromGraveyard] over `withCycling()` — cast only, so a land with
 *   cycling can't be played and a card can't be cycled from the graveyard (ruling).
 * - The exile half is Rest in Peace's graveyard redirect narrowed to cycling cards you own and to
 *   [ZoneChangeCause.NotCycled]: the card discarded to pay its own cycling (or typecycling) cost
 *   still reaches the graveyard; every other route there — discard to an effect, mill, dying,
 *   resolving, countered — is exiled. Tokens aren't cards, so a token with cycling goes to the
 *   graveyard (ruling); in a zone-change filter `withCycling()` fails for a permanent that lost all
 *   its abilities or is face down, so it goes there too.
 */
val AbandonedSarcophagus = card("Abandoned Sarcophagus") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "You may cast spells that have a cycling ability from your graveyard.\n" +
        "If a card that has a cycling ability would be put into your graveyard from anywhere and it wasn't cycled, exile it instead."

    staticAbility {
        ability = MayCastFromGraveyard(filter = GameObjectFilter.Any.withCycling())
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Any.withCycling().nontoken().ownedByYou(),
                to = Zone.GRAVEYARD,
            ),
            requiredCause = ZoneChangeCause.NotCycled,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Daarken"
        flavorText = "Although long since dead, the banished dissenters finally took their vengeance."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c191fb87-f111-4ac2-a52a-3af103a9314e.jpg?1783936003"
        ruling("2017-07-14", "A token permanent with cycling will go to your graveyard before ceasing to exist. It won't be exiled. Similarly, a nontoken permanent that lost cycling while it was on the battlefield will also go to your graveyard.")
        ruling("2017-07-14", "If you cycle an instant card, you may cast it from your graveyard right away before any player may take any other action. If you do, that spell will resolve before the cycling ability. If you cycle a noninstant card without flash, you can't cast it until after the cycling ability has resolved.")
        ruling("2017-07-14", "Abandoned Sarcophagus doesn't grant you permission to do anything with those cards except cast them. For example, you can't cycle nonland cards with cycling from your graveyard.")
        ruling("2017-07-14", "Certain older cards have variants of cycling, such as basic landcycling or Wizardcycling. Abandoned Sarcophagus's effect lets you cast these cards and exiles them if they weren't discarded for their cycling variant's ability.")
    }
}
