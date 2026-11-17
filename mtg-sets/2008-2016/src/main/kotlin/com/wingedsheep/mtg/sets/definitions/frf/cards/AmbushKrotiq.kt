package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ambush Krotiq
 * {5}{G}
 * Creature — Insect
 * 5/5
 *
 * Trample
 * When this creature enters, return another creature you control to its owner's hand.
 *
 * The bounce doesn't target (2014-11-24 ruling: the creature is chosen as the trigger resolves),
 * so it is a gather → `chooseExactly(1)` → `toHand` pipeline excluding the Krotiq itself. With no
 * other creature the gather is empty and nothing happens; the Krotiq stays.
 */
val AmbushKrotiq = card("Ambush Krotiq") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 5
    toughness = 5
    oracleText = "Trample\nWhen this creature enters, return another creature you control to its owner's hand."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, return another creature you control to its owner's hand."
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                    excludeSelf = true
                )
            )
            val bounced = chooseExactly(
                1,
                from = candidates,
                prompt = "Return another creature you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(bounced)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Filip Burburan"
        flavorText = "The Abzan merchants soon discovered why this route was clear of dragons."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd3068c6-d403-46d5-b3d7-1c3b00b9a9b1.jpg?1783938682"
        ruling(
            "2014-11-24",
            "If you control no other creatures as Ambush Krotiq's triggered ability resolves, nothing " +
                "happens. Ambush Krotiq remains on the battlefield."
        )
        ruling(
            "2014-11-24",
            "The triggered ability doesn't target any permanent. You choose which one to return as the " +
                "ability resolves. No player can respond to this choice once the ability starts resolving."
        )
    }
}
