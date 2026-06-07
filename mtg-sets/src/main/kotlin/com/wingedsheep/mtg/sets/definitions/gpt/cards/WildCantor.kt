package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Wild Cantor
 * {R/G}
 * Creature — Human Druid
 * 1/1
 *
 * ({R/G} can be paid with either {R} or {G}.)
 * Sacrifice this creature: Add one mana of any color.
 */
val WildCantor = card("Wild Cantor") {
    manaCost = "{R/G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Druid"
    oracleText = "({R/G} can be paid with either {R} or {G}.)\n" +
        "Sacrifice this creature: Add one mana of any color."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Glenn Fabry"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/242dc29e-d8f5-4207-abbf-cf5425f08551.jpg?1593272918"
    }
}
