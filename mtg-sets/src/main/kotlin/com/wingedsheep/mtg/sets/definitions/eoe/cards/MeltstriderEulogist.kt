package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Meltstrider Eulogist
 * {2}{G}
 * Creature — Insect Soldier
 * 3/3
 *
 * Whenever a creature you control with a +1/+1 counter on it dies, draw a card.
 */
val MeltstriderEulogist = card("Meltstrider Eulogist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Soldier"
    power = 3
    toughness = 3
    oracleText = "Whenever a creature you control with a +1/+1 counter on it dies, draw a card."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().withCounter(Counters.PLUS_ONE_PLUS_ONE),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = Effects.DrawCards(1)
        description = "Whenever a creature you control with a +1/+1 counter on it dies, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Jason A. Engle"
        flavorText = "There are no failures in terrasymbiosis, only lessons learned."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df61aa0c-effc-4d57-be19-876a82c41d33.jpg?1752947359"

        ruling(
            "2025-07-25",
            "If Meltstrider Eulogist dies at the same time as one or more creatures you control with " +
                "+1/+1 counters on them, its ability will trigger for each of those creatures. This " +
                "includes Meltstrider Eulogist itself if it had a +1/+1 counter on it when it died."
        )
    }
}
