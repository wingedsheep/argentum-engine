package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Indomitable Creativity
 * {X}{R}{R}{R}
 * Sorcery
 *
 * Destroy X target artifacts and/or creatures. For each permanent destroyed this way, its
 * controller reveals cards from the top of their library until an artifact or creature card is
 * revealed and exiles that card. Those players put the exiled cards onto the battlefield, then
 * shuffle.
 *
 * The controllers are captured before the destroy, so "its controller" is whoever controlled the
 * permanent as it was destroyed, and only permanents actually destroyed are tallied — an
 * indestructible or regenerated target counts for nothing, a destroyed one Rest in Peace exiles
 * still counts. Each tallied controller walks their library once for as many artifact/creature
 * cards as they lost permanents (the same cards a reveal-per-permanent would find) and exiles
 * them linked to the spell; every exiled card then enters at once, and only those players shuffle.
 */
val IndomitableCreativity = card("Indomitable Creativity") {
    manaCost = "{X}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy X target artifacts and/or creatures. For each permanent destroyed this way, " +
        "its controller reveals cards from the top of their library until an artifact or creature card " +
        "is revealed and exiles that card. Those players put the exiled cards onto the battlefield, " +
        "then shuffle."

    spell {
        targets(TargetFilter.CreatureOrArtifact, exactly = DynamicAmounts.xValue())
        effect = Effects.Pipeline {
            val targets = gather(CardSource.ChosenTargets)
            val controllers = captureControllers(targets)
            val destroyed = moveTracked(
                targets,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
            forEachCaptured(destroyed, original = targets, controllers = controllers) { lost ->
                val (found, revealed) = gatherUntilMatch(
                    GameObjectFilter.CreatureOrArtifact,
                    count = lost.amount
                )
                reveal(revealed, fromZone = Zone.LIBRARY, toZone = Zone.EXILE)
                exile(found, linkToSource = true)
            }
            val exiled = gather(CardSource.FromLinkedExile())
            move(exiled, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
            forEachCaptured(destroyed, original = targets, controllers = controllers) {
                run(Effects.ShuffleLibrary(EffectTarget.Controller))
            }
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "85"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edd00e45-2ae1-4cd0-92a1-155c95f8dc72.jpg?1783936753"
        ruling("2017-02-09", "If a player controls no permanents destroyed this way, that player reveals no cards from their library and doesn't shuffle it.")
        ruling("2017-02-09", "If an artifact or creature is targeted but not destroyed (perhaps because it gained indestructible or became an illegal target), it doesn't count as one of the artifacts or creatures destroyed this way. An artifact or creature that's destroyed but put into a zone other than a graveyard (for example, due to the effect of Rest in Peace) does count.")
        ruling("2017-02-09", "While revealing cards, a player stops as soon as they reveal a card that's an artifact or a creature (or both). That player doesn't choose one type.")
        ruling("2017-02-09", "If a player's library no longer contains an artifact or creature card when instructed to reveal cards, that player reveals the entire library, exiles no cards, and then shuffles it.")
        ruling("2017-02-09", "All of the exiled cards are put onto the battlefield at the same time.")
    }
}
