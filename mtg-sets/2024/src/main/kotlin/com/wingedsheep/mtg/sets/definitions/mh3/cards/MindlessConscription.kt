package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mindless Conscription
 * {2}{B}
 * Enchantment
 *
 * When this enchantment enters and whenever you draw your third card each turn, amass Zombies 3.
 *
 * "When … and whenever …" is two triggers sharing one effect (CR 603.2): an ETB trigger and a
 * draw-count trigger that fires once when your third draw of the turn happens.
 */
val MindlessConscription = card("Mindless Conscription") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters and whenever you draw your third card each turn, amass Zombies 3. " +
        "(Put three +1/+1 counters on an Army you control. It's also a Zombie. If you don't control an Army, " +
        "create a 0/0 black Zombie Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Amass(3, "Zombie")
    }

    triggeredAbility {
        trigger = Triggers.you.drawsNth(3)
        effect = Effects.Amass(3, "Zombie")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1af5b195-101a-4265-98a7-522a968cf218.jpg?1783911278"
    }
}
