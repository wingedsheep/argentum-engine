package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Galvanic Alchemist — Avacyn Restored #54 (canonical printing)
 * {2}{U} · Creature — Human Wizard · 1/4
 *
 * Soulbond
 * As long as this creature is paired with another creature, each of those creatures has
 * "{2}{U}: Untap this creature."
 *
 * [Stonewright]'s shape with an untap: [GrantActivatedAbility] over `GroupFilter.soulbondPair()`.
 * The granted ability's [EffectTarget.Self] is the creature it was activated on, so each half
 * untaps itself.
 */
val GalvanicAlchemist = card("Galvanic Alchemist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as this creature is paired with another creature, each of those creatures has " +
        "\"{2}{U}: Untap this creature.\""
    power = 1
    toughness = 4

    soulbond()

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{2}{U}")
                effect = Effects.Untap(EffectTarget.Self)
                description = "{2}{U}: Untap this creature."
            },
            filter = GroupFilter.soulbondPair()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0e24d65-0e6f-4978-8de1-c5e4acac12fb.jpg?1783940721"
    }
}
