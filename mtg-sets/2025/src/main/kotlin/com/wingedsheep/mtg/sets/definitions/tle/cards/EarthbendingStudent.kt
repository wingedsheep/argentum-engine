package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Earthbending Student
 * {2}{G}
 * Creature — Human Warrior Ally
 * 1/3
 * When this creature enters, earthbend 2.
 * Land creatures you control have vigilance.
 *
 * The ETB is the standard [Effects.Earthbend] over a targeted land you control. The static is a
 * [GrantKeyword] over land creatures you control — Tatyova, Steward of Tides' shape with vigilance.
 */
val EarthbendingStudent = card("Earthbending Student") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior Ally"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, earthbend 2. (Target land you control becomes a 0/0 " +
        "creature with haste that's still a land. Put two +1/+1 counters on it. When it dies or is " +
        "exiled, return it to the battlefield tapped.)\n" +
        "Land creatures you control have vigilance. (Attacking doesn't cause them to tap.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(2, land)
        description = "When this creature enters, earthbend 2."
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Land).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Le Vuong"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f068ce9-f4c2-4d40-bfa3-7f21f6d3b0b7.jpg?1783904775"
        ruling("2025-10-02", "You may target a land that is already a creature, perhaps because of a previous earthbend ability. The land will get the +1/+1 counters, gain haste, and have its base power and toughness set to 0/0.")
    }
}
