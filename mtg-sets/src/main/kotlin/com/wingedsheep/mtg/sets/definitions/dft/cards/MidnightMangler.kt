package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Midnight Mangler — Aetherdrift #50
 * {1}{U} · Artifact — Vehicle · 3/3
 *
 * During turns other than yours, this Vehicle is an artifact creature.
 * Crew 2
 *
 * A free blocker that switches itself on: the animation is a Layer 4 type-adding static
 * ([GrantCardType]) gated by [Conditions.IsNotYourTurn], not a triggered ability. Because
 * [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] is re-evaluated on every projection, the
 * Vehicle becomes a creature the instant the turn passes to an opponent and stops being one when
 * your turn begins again — including mid-turn control changes, since the condition is read against
 * the ability's current controller.
 *
 * The card is already an Artifact, so adding CREATURE is the whole type change; the printed 3/3
 * needs no help. Crew 2 still works on your own turn for attacking.
 */
val MidnightMangler = card("Midnight Mangler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "During turns other than yours, this Vehicle is an artifact creature.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    staticAbility {
        condition = Conditions.IsNotYourTurn
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Villarrte"
        flavorText = "\"Is someone asleep at the wheel, or are the wheels driving themselves?!\"\n" +
            "—Vnwxt, Grand Prix host"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/237568e3-7331-4bbb-a091-a766723134fc.jpg?1783907906"
    }
}
