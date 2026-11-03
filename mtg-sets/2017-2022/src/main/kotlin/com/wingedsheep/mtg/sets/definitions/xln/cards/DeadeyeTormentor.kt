package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deadeye Tormentor
 * {2}{B}
 * Creature — Human Pirate
 * 2/2
 * Raid — When this creature enters, if you attacked this turn, target opponent discards a card.
 *
 * Raid is the intervening-if [Conditions.YouAttackedThisTurn] on the enters trigger (see Storm Fleet Spy).
 */
val DeadeyeTormentor = card("Deadeye Tormentor") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "Raid — When this creature enters, if you attacked this turn, target opponent discards a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(1, opponent)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Eric Deschamps"
        flavorText = "Despite their noble heritage, the pirates of the Deadeye Fleet are known for underhanded tactics and despicable trickery."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d219fb0a-49a3-4711-9b37-a0c1b36f68d0.jpg?1783935764"
    }
}
