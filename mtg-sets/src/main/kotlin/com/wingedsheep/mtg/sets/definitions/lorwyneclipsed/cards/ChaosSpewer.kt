package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect

/**
 * Chaos Spewer
 * {2}{B/R}
 * Creature — Goblin Warlock
 * 5/4
 *
 * When this creature enters, you may pay {2}. If you don't, blight 2.
 * (To blight 2, put two -1/-1 counters on a creature you control.)
 */
val ChaosSpewer = card("Chaos Spewer") {
    manaCost = "{2}{B/R}"
    typeLine = "Creature — Goblin Warlock"
    power = 5
    toughness = 4
    oracleText = "When this creature enters, you may pay {2}. If you don't, blight 2. " +
        "(To blight 2, put two -1/-1 counters on a creature you control.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = PayOrSufferEffect(
            cost = PayCost.Mana(ManaCost.parse("{2}")),
            suffer = EffectPatterns.blight(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Quintin Gleim"
        flavorText = "Only the most powerful boggart cursecrafters seek out veins of wild magic to fuel their vile hexes."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5918fa5-1d13-447b-8838-633b6b61e791.jpg?1767749712"
    }
}
