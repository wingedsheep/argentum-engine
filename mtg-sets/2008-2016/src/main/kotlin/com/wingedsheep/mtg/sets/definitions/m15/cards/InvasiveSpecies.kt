package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invasive Species — Magic 2015 #181
 * {2}{G} · Creature — Insect · 3/3
 *
 * When this creature enters, return another permanent you control to its owner's hand.
 *
 * Aviary Mechanic's shape without the "may": the bounce doesn't target (2014-07-18 ruling — the
 * permanent is chosen as the trigger resolves), so it is a gather → `chooseExactly(1)` → `toHand`
 * pipeline excluding Invasive Species itself. With no other permanent the gather is empty and
 * nothing happens.
 */
val InvasiveSpecies = card("Invasive Species") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, return another permanent you control to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        description = "When this creature enters, return another permanent you control to its owner's hand."
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Permanent,
                    player = Player.You,
                    excludeSelf = true
                )
            )
            val bounced = chooseExactly(
                1,
                from = candidates,
                prompt = "Return another permanent you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(bounced)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Christopher Moeller"
        flavorText = "It's easier to relocate a village that lies in their path than to turn the bugs aside."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1fb5a98-94f4-419f-850b-e3a01f17080f.jpg?1783939166"
        ruling(
            "2014-07-18",
            "The ability doesn't target any permanent. You choose which one to return when the ability " +
                "resolves. No player can respond to this choice once the ability starts resolving."
        )
    }
}
