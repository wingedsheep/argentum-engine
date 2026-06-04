package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Naban, Dean of Iteration
 * {1}{U}
 * Legendary Creature — Human Wizard
 * 2/1
 * If a Wizard entering the battlefield under your control causes a triggered ability
 * of a permanent you control to trigger, that ability triggers an additional time.
 */
val NabanDeanOfIteration = card("Naban, Dean of Iteration") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "If a Wizard entering the battlefield under your control causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time."

    staticAbility {
        ability = AdditionalETBOrLTBTriggers(
            filter = GameObjectFilter.Creature.withSubtype("Wizard"),
            description = "If a Wizard entering the battlefield under your control causes a triggered ability of a permanent you control to trigger, that ability triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Ryan Alexander Lee"
        flavorText = "\"Perfect. Now do it again.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88f41175-880f-491e-96c3-bf52f3c0db5d.jpg?1722108748"
    }
}
