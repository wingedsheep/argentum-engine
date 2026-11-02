package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fell Specter
 * {3}{B}
 * Creature — Specter
 * 1/3
 * Flying
 * When this creature enters, target opponent discards a card.
 * Whenever an opponent discards a card, that player loses 2 life.
 */
val FellSpecter = card("Fell Specter") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, target opponent discards a card.\n" +
        "Whenever an opponent discards a card, that player loses 2 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(1, opponent)
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.discards()
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent discards a card, that player loses 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Dimitar Marinski"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8cb52a4-5fec-41a0-8030-503a61e3d33e.jpg?1783934570"
    }
}
