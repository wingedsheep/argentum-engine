package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Chillerpillar
 * {3}{U}
 * Snow Creature — Insect
 * 3/3
 * {4}{S}{S}: Monstrosity 2.
 * As long as this creature is monstrous, it has flying.
 */
val Chillerpillar = card("Chillerpillar") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Snow Creature — Insect"
    power = 3
    toughness = 3
    oracleText = "{4}{S}{S}: Monstrosity 2. (If this creature isn't monstrous, put two +1/+1 counters on it " +
        "and it becomes monstrous. {S} can be paid with one mana from a snow source.)\n" +
        "As long as this creature is monstrous, it has flying."

    activatedAbility {
        cost = Costs.Mana("{4}{S}{S}")
        effect = Effects.Monstrosity(2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
        condition = Conditions.SourceIsMonstrous
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Suzanne Helmigh"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f57005c-414d-4c83-9b4f-cd26e547d54d.jpg?1783933149"

        ruling("2019-06-14", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2019-06-14", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature, loses its abilities, or loses its +1/+1 counters, it will continue to be monstrous.")
        ruling("2019-06-14", "Chillerpillar gaining flying after it becomes blocked won't remove the blocking creature from combat or cause Chillerpillar to become unblocked.")
    }
}
