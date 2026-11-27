package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Geralf's Messenger
 * {B}{B}{B}
 * Creature — Zombie
 * 3/2
 * This creature enters tapped.
 * When this creature enters, target opponent loses 2 life.
 * Undying
 */
val GeralfsMessenger = card("Geralf's Messenger") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 2
    oracleText = "This creature enters tapped.\n" +
        "When this creature enters, target opponent loses 2 life.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"

    keywords(Keyword.UNDYING)

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bffaad78-97ff-431f-bfb0-e96c7558f974.jpg"
    }
}
