package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fearsome Goblin Pair
 * {2}{B/R}
 * Creature — Goblin Soldier
 * 1/1
 *
 * When this creature dies, amass Goblins 4.
 */
val FearsomeGoblinPair = card("Fearsome Goblin Pair") {
    manaCost = "{2}{B/R}"
    colorIdentity = "BR"
    typeLine = "Creature — Goblin Soldier"
    oracleText = "When this creature dies, amass Goblins 4. (Put four +1/+1 counters on an Army you control. " +
        "It's also a Goblin. If you don't control an Army, create a 0/0 black Goblin Army creature token first.)"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Amass(4, "Goblin")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Michele Giorgi"
        flavorText = "Out jumped the Goblins: big Goblins, small Goblins, great ugly-looking Goblins, lots of Goblins!"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2efe2dc7-3eaa-47f6-b1ae-f974c4a8ae79.jpg?1785324572"
    }
}
