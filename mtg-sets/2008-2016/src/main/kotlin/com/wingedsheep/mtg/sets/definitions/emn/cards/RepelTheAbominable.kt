package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Repel the Abominable
 * {1}{W}
 * Instant
 *
 * Prevent all damage that would be dealt this turn by non-Human sources.
 *
 * The Ethereal Haze shield — all damage, not just combat, on the *source* side — over every
 * source that isn't a Human rather than over creatures. `GameObjectFilter.Any` keeps noncreature
 * sources in (a burn spell, an artifact's ability): per the ruling, a source with no creature type
 * at all is a non-Human source. The filter is read as each damage instance would be dealt, so a
 * Human Werewolf is spared while it's a Human and covered once it isn't.
 */
val RepelTheAbominable = card("Repel the Abominable") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all damage that would be dealt this turn by non-Human sources."

    spell {
        effect = Effects.PreventDamage(
            direction = PreventionDirection.FromTarget,
            sources = PreventionSourceFilter.Matching(GameObjectFilter.Any.notSubtype(Subtype.HUMAN))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Dan Murayama Scott"
        flavorText = "\"Fight for a better future, even if you may not live to see it.\"\n—Grete, Order of Saint Traft"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf153f4d-0d37-41a9-80d8-7f2adaad79c7.jpg?1783937511"
        ruling("2016-07-13", "Sources that have no creature type at all are non-Human sources, as are sources with creature types that don't include Human. A creature that is Human and has other types, such as a Human Werewolf, is still a Human source.")
        ruling("2016-07-13", "If a spell or ability causes damage to be dealt, it will indicate what object deals the damage. The object dealing damage might not be the spell itself.")
    }
}
