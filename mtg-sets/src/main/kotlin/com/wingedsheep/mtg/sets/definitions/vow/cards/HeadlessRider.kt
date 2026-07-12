package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Headless Rider
 * {2}{B}
 * Creature — Zombie
 * 3/1
 *
 * Whenever this creature or another nontoken Zombie you control dies, create a 2/2 black Zombie
 * creature token.
 *
 * A dies trigger scoped to nontoken Zombies you control with [TriggerBinding.ANY] — Headless Rider
 * is itself a nontoken Zombie you control, so the same filter covers both "this creature" and
 * "another nontoken Zombie you control." (Zombie tokens it produces are excluded by `nontoken()`.)
 */
val HeadlessRider = card("Headless Rider") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 1
    oracleText = "Whenever this creature or another nontoken Zombie you control dies, create a 2/2 " +
        "black Zombie creature token."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.withSubtype("Zombie").youControl().nontoken(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie")
        )
        description = "Whenever this creature or another nontoken Zombie you control dies, create " +
            "a 2/2 black Zombie creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "372"
        artist = "E. M. Gist"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/544d7162-b78f-4a70-86e7-83cec7062039.jpg?1782702926"
    }
}
