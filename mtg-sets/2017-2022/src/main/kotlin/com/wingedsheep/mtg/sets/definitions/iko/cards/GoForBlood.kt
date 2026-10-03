package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Go for Blood
 * {1}{R}
 * Sorcery
 *
 * Target creature you control fights target creature you don't control.
 * Cycling {1}
 *
 * "You don't control" is `Not(ControlledByYou)` rather than "an opponent controls" — the same
 * spelling as Swift Kick.
 */
val GoForBlood = card("Go for Blood") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature you control fights target creature you don't control. " +
        "(Each deals damage equal to its power to the other.)\n" +
        "Cycling {1} ({1}, Discard this card: Draw a card.)"

    spell {
        val yourCreature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        val theirCreature = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Fight(yourCreature, theirCreature)
    }

    keywordAbility(KeywordAbility.cycling("{1}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Chris Rallis"
        flavorText = "Lukka's bond provided not only friendship, but also the most powerful weapon in his arsenal."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67315df9-99a1-45ba-8ade-ddc476368a86.jpg"
    }
}
