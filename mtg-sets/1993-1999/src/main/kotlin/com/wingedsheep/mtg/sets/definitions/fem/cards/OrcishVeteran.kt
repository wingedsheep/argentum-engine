package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Orcish Veteran
 * {2}{R}
 * Creature — Orc
 * 2/2
 * This creature can't block white creatures with power 2 or greater.
 * {R}: This creature gains first strike until end of turn.
 *
 * Like [BrassclawOrcs], the blocking restriction is written as its complement: everything it may
 * block is either non-white, or has power 1 or less.
 */
val OrcishVeteran = card("Orcish Veteran") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc"
    oracleText = "This creature can't block white creatures with power 2 or greater.\n" +
        "{R}: This creature gains first strike until end of turn."
    power = 2
    toughness = 2

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(
            GameObjectFilter.Creature.notColor(Color.WHITE) or GameObjectFilter.Creature.powerAtMost(1)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62a"
        artist = "Douglas Shuler"
        flavorText = "Orcs are not exactly known for their valor—although most Orcs have seen countless battles, only a handful have actually fought in them."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dbca765-8756-4e28-9faf-25714c9b8838.jpg?1783947891"
    }
}
