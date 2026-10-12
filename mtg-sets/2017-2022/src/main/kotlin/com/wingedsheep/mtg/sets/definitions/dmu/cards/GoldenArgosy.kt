package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Golden Argosy
 * {4}
 * Legendary Artifact — Vehicle
 * 3/6
 * Whenever Golden Argosy attacks, exile each creature that crewed it this turn. Return them to
 * the battlefield tapped under their owner's control at the beginning of the next end step.
 * Crew 1
 *
 * The crewers come off the Vehicle's crew record ([CardSource.CreaturesThatCrewedOrSaddledSource]),
 * which outlives the Argosy leaving the battlefield, and only those still on the battlefield are
 * exiled. The end-step trigger carries the cards actually exiled, so it returns them even if the
 * Argosy is gone by then.
 */
val GoldenArgosy = card("Golden Argosy") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact — Vehicle"
    oracleText = "Whenever Golden Argosy attacks, exile each creature that crewed it this turn. " +
        "Return them to the battlefield tapped under their owner's control at the beginning of the next end step.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"
    power = 3
    toughness = 6

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val crewers = gather(CardSource.CreaturesThatCrewedOrSaddledSource)
            val exiled = moveTracked(crewers, CardDestination.ToZone(Zone.EXILE), name = "argosyExiled")
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Pipeline {
                    move(
                        exiled,
                        CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                        underOwnersControl = true
                    )
                },
                carryCollections = listOf(exiled.key)
            ))
        }
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8611a53-e54e-4625-8e28-03511f1550e8.jpg?1783921270"
        ruling("2022-09-09", "Golden Argosy's triggered ability will only exile creatures that are still on the battlefield as it resolves, and any creatures that crewed it this turn that had already left the battlefield will not be returned.")
        ruling("2022-09-09", "The exiled creatures will be returned at the beginning of the next end step even if Golden Argosy is no longer on the battlefield at that time.")
    }
}
