package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Rayblade Trooper
 * {2}{W}
 * Creature — Human Soldier
 * 2/2
 * When this creature enters, put a +1/+1 counter on target creature you control.
 * Whenever a nontoken creature you control with a +1/+1 counter on it dies, create a 1/1 white
 * Human Soldier creature token.
 * Warp {1}{W}
 */
val RaybladeTrooper = card("Rayblade Trooper") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, put a +1/+1 counter on target creature you control.\n" +
        "Whenever a nontoken creature you control with a +1/+1 counter on it dies, create a 1/1 white Human Soldier creature token.\n" +
        "Warp {1}{W} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val target = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, target)
        description = "When this creature enters, put a +1/+1 counter on target creature you control."
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().nontoken()
                .withCounter(Counters.PLUS_ONE_PLUS_ONE),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            count = 1,
            imageUri = "https://cards.scryfall.io/normal/front/6/3/631c2c16-132d-4607-ab7e-207a6af188e5.jpg?1757686920"
        )
        description = "Whenever a nontoken creature you control with a +1/+1 counter on it dies, create a 1/1 white Human Soldier creature token."
    }

    warp = "{1}{W}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c08c7bf9-a2ed-45c6-8b48-15122d9d9e37.jpg?1752946672"
        ruling(
            "2025-07-25",
            "If Rayblade Trooper dies at the same time as one or more creatures you control with +1/+1 counters on them, its second ability will trigger for each of those creatures. This includes Rayblade Trooper itself if it had a +1/+1 counter on it when it died."
        )
    }
}
