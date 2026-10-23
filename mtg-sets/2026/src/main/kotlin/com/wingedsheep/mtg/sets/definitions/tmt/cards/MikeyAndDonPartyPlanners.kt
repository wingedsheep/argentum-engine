package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Mikey & Don, Party Planners
 * {2}{G/U}{G/U}
 * Legendary Creature — Mutant Ninja Turtle
 * 3/3
 *
 * Ward {2}
 * You may look at the top card of your library any time.
 * You may play lands and cast Mutant, Ninja, or Turtle spells from the top of your
 * library. If you cast a creature spell this way, that creature enters with an
 * additional +1/+1 counter on it.
 */
val MikeyAndDonPartyPlanners = card("Mikey & Don, Party Planners") {
    manaCost = "{2}{G/U}{G/U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Ward {2}\nYou may look at the top card of your library any time.\nYou may play lands and cast Mutant, Ninja, or Turtle spells from the top of your library. If you cast a creature spell this way, that creature enters with an additional +1/+1 counter on it."
    power = 3
    toughness = 3

    keywords(Keyword.WARD)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    staticAbility { ability = LookAtTopOfLibrary() }
    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(
            spellFilter = GameObjectFilter.Any.withAnyOfSubtypes(
                listOf(Subtype("Mutant"), Subtype("Ninja"), Subtype("Turtle"))
            )
        )
    }

    // "If you cast a creature spell this way, it enters with an additional +1/+1 counter" — a
    // selfOnly = false EntersWithCounters gated on the entering creature being cast from the
    // library (CastFromLibraryComponent + WasCastFromZone(LIBRARY)).
    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            selfOnly = false,
            condition = Conditions.WasCastFromZone(Zone.LIBRARY)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Gabriel Rubio"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6353df1a-9a1b-41fd-985b-8c8acba36c23.jpg?1769006263"
    }
}
