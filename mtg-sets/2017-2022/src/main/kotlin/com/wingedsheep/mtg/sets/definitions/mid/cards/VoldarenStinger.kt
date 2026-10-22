package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voldaren Stinger
 * {R}
 * Creature — Vampire Warrior
 * 1/1
 * This creature has first strike as long as it's attacking.
 * {2}{R}: This creature gets +2/+0 until end of turn.
 */
val VoldarenStinger = card("Voldaren Stinger") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Warrior"
    power = 1
    toughness = 1
    oracleText = "This creature has first strike as long as it's attacking.\n" +
        "{2}{R}: This creature gets +2/+0 until end of turn."

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source())
        condition = Conditions.SourceIsAttacking
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
        description = "This creature gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Martina Fačková"
        flavorText = "A duel to first blood means something very different when a vampire is involved."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f27a7c78-3a64-42fc-8808-15b1f41976a1.jpg?1783925587"
    }
}
