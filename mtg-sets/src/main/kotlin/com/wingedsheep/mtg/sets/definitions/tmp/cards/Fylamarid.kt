package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Fylamarid
 * {1}{U}{U}
 * Creature — Squid Beast
 * 1/3
 *
 * Flying
 * This creature can't be blocked by blue creatures.
 * {U}: Target creature becomes blue until end of turn.
 */
val Fylamarid = card("Fylamarid") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Squid Beast"
    power = 1
    toughness = 3
    oracleText = "Flying\n" +
        "This creature can't be blocked by blue creatures.\n" +
        "{U}: Target creature becomes blue until end of turn."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.BLUE))
    }

    activatedAbility {
        cost = Costs.Mana("{U}")
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ChangeColor(creature, colors = setOf(Color.BLUE))
        description = "{U}: Target creature becomes blue until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Una Fricker"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dd4f686-79e3-4067-81f9-7fae0c25dc8f.jpg?1562055416"
    }
}
