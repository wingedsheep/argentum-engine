package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Shrewd Negotiation
 * {4}{U}
 * Sorcery
 *
 * Exchange control of target artifact you control and target artifact or creature you don't
 * control.
 *
 * The Chromeshell Crab shape — [Effects.ExchangeControl] over a "you control" slot and a "you
 * don't control" slot — with artifact filters. "You don't control" is `Not(ControlledByYou)`
 * rather than `opponentControls()` (the In Garruk's Wake / Hunter's Mark spelling). Both targets
 * are re-checked on resolution, so the exchange doesn't happen if you no longer control your
 * artifact or have since gained control of the other permanent.
 */
val ShrewdNegotiation = card("Shrewd Negotiation") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Exchange control of target artifact you control and target artifact or creature you don't control."

    spell {
        val yourArtifact = target(TargetFilter(GameObjectFilter.Artifact.youControl()))
        val theirs = target(
            TargetFilter(
                (GameObjectFilter.Artifact or GameObjectFilter.Creature).withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ExchangeControl(yourArtifact, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Slawomir Maniak"
        flavorText = "\"Pleasure doing business with you.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37900980-c2e2-4b70-8511-405f0a8389cd.jpg?1783937216"

        ruling("2016-09-20", "If one of the target permanents is an illegal target as Shrewd Negotiation resolves, the exchange won't happen.")
        ruling("2016-09-20", "Gaining control of an Equipment doesn't cause it to move, though it will allow you to activate its equip ability later to attach it to a creature you control.")
    }
}
