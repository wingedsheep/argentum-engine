package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ancestral Statue
 * {4}
 * Artifact Creature — Golem
 * 3/4
 * When this creature enters, return a nonland permanent you control to its owner's hand.
 *
 * Not targeted — the permanent is chosen on resolution (gather → select → move, as Emancipation
 * Angel). Mandatory: if the Statue is your only nonland permanent, it returns itself.
 */
val AncestralStatue = card("Ancestral Statue") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    oracleText = "When this creature enters, return a nonland permanent you control to its owner's hand."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val yourPermanents = gather(
                CardSource.BattlefieldMatching(filter = GameObjectFilter.NonlandPermanent, player = Player.You)
            )
            val returned = chooseExactly(
                1,
                from = yourPermanents,
                prompt = "Choose a nonland permanent you control to return to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "234"
        artist = "Tomasz Jedruszek"
        flavorText = "The mage awakened the statue in hopes of learning the lost lore of her clan, but the statue was interested only in war."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7124a6f-b690-4326-93b5-036a8c520c1f.jpg?1783938569"
        ruling("2015-02-25", "Ancestral Statue's ability is mandatory. If Ancestral Statue is the only nonland permanent you control when its ability resolves, you must return it to its owner's hand.")
        ruling("2015-02-25", "The triggered ability doesn't target any permanent. You choose which one to return as the ability resolves. No player can respond to this choice once the ability starts resolving.")
    }
}
