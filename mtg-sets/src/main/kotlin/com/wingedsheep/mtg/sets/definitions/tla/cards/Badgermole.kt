package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Badgermole
 * {4}{G}
 * Creature — Badger Mole
 * 4/4
 * When this creature enters, earthbend 2.
 * Creatures you control with +1/+1 counters on them have trample.
 */
val Badgermole = card("Badgermole") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Badger Mole"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, earthbend 2. (Target land you control becomes a 0/0 creature with haste that's still a land. Put two +1/+1 counters on it. When it dies or is exiled, return it to the battlefield tapped.)\n" +
        "Creatures you control with +1/+1 counters on them have trample."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target("target land you control", TargetObject(filter = TargetFilter.Land.youControl()))
        effect = Effects.Earthbend(2, land)
        description = "When this creature enters, earthbend 2."
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.youControl().withCounter(Counters.PLUS_ONE_PLUS_ONE)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Matteo Bassini"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aabbd420-b7fc-496f-9168-bad823d51d9e.jpg?1764121134"
    }
}
