package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Hunted Nightmare
 * {1}{B}{B}
 * Creature — Nightmare
 * 4/5
 * Menace
 * When this creature enters, target opponent puts a deathtouch counter on a creature they control.
 *
 * The targeted opponent picks the creature (`Chooser.TargetPlayer`), chosen on the battlefield at
 * resolution — the Chittering Rats shape with Call of the Death-Dweller's counter step. An opponent
 * with no creatures gathers nothing and the trigger does nothing.
 */
val HuntedNightmare = card("Hunted Nightmare") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare"
    power = 4
    toughness = 5
    oracleText = "Menace\n" +
        "When this creature enters, target opponent puts a deathtouch counter on a creature they control."

    keywords(Keyword.MENACE)

    triggeredAbility {
        val opponent = target(Targets.Opponent)
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val theirCreatures = gather(
                CardSource.ControlledPermanents(
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Creature
                )
            )
            val host = chooseExactly(
                1,
                from = theirCreatures,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose a creature you control to put a deathtouch counter on",
                useTargetingUI = true
            )
            run(Effects.AddCountersToCollection(host, CounterType.DEATHTOUCH))
        }
        description = "When this creature enters, target opponent puts a deathtouch counter on " +
            "a creature they control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Antonio José Manzanedo"
        flavorText = "\"The cruelest monsters inspire the greatest tales. The greatest tales inspire the cruelest hunters.\"\n—Rielle, the Everwise"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f2dddce-583e-4af3-b144-a60dd268e84f.jpg?1783931060"
        ruling("2020-04-17", "The target opponent chooses which of their creatures receives a deathtouch counter.")
        ruling("2020-04-17", "If the target opponent controls no creatures, they won't put a deathtouch counter on any creature.")
    }
}
