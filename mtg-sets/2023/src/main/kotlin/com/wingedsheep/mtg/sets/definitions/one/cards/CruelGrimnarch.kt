package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cruel Grimnarch
 * {5}{B}
 * Creature — Phyrexian Cleric
 * 5/5
 * Deathtouch
 * When this creature enters, each opponent discards a card. For each opponent who can't, you gain 4 life.
 *
 * "Each opponent who can't" is an opponent with no cards in hand when the ability resolves. Those
 * opponents are recorded before anyone discards (`storePlayer(onlyIf = EmptyHand)` per opponent),
 * so an opponent who discards their last card doesn't count. The life gain sits outside the
 * per-opponent iteration, so "you" stays the ability's controller.
 */
val CruelGrimnarch = card("Cruel Grimnarch") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Cleric"
    oracleText = "Deathtouch\nWhen this creature enters, each opponent discards a card. " +
        "For each opponent who can't, you gain 4 life."
    power = 5
    toughness = 5

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val cant = forEachPlayerCollecting(Player.EachOpponent) {
                listOf(storePlayer(onlyIf = Conditions.EmptyHand))
            }.single()
            run(Effects.EachOpponentDiscards(1))
            run(Effects.GainLife(cant.count * 4))
        }
        description = "When this creature enters, each opponent discards a card. " +
            "For each opponent who can't, you gain 4 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Allen Williams"
        flavorText = "The gospel of lies sounds sweetest when whispered by many mouths."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3f571b5-ce50-4856-a151-fe6610d27bca.jpg?1783918048"
    }
}
