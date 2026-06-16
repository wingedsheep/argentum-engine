package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Warbeast of Gorgoroth
 * {4}{R}
 * Creature — Beast
 * 5/4
 *
 * Whenever this creature or another creature you control with power 4 or greater dies, amass Orcs 2.
 */
val WarbeastOfGorgoroth = card("Warbeast of Gorgoroth") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 4
    oracleText = "Whenever this creature or another creature you control with power 4 or greater dies, " +
        "amass Orcs 2. (Put two +1/+1 counters on an Army you control. It's also an Orc. If you don't " +
        "control an Army, create a 0/0 black Orc Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().powerAtLeast(4),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.Amass(2, "Orc")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        flavorText = "Drums rolled, fires leaped, and great engines drawn by beasts crawled across the field."
        artist = "Oleg Shekhovtsov"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5eeecb9e-fbde-429b-b3da-18a4dbadf6de.jpg?1686969217"
    }
}
