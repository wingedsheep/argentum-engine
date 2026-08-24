package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Order of the Ebon Hand
 * {B}{B}
 * Creature — Cleric Knight
 * 2/1
 * Protection from white
 * {B}: This creature gains first strike until end of turn.
 * {B}{B}: This creature gets +1/+0 until end of turn.
 *
 * The white mirror of [OrderOfLeitbur].
 */
val OrderOfTheEbonHand = card("Order of the Ebon Hand") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Cleric Knight"
    oracleText = "Protection from white\n" +
        "{B}: This creature gains first strike until end of turn.\n" +
        "{B}{B}: This creature gets +1/+0 until end of turn."
    power = 2
    toughness = 1

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.WHITE)))

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{B}{B}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42a"
        artist = "Melissa A. Benson"
        flavorText = "A true follower of Tourach took pride in achievement to the exclusion of other concerns."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e51f5d8-a7cc-4720-8af5-e002bcfd78a0.jpg?1783947899"
    }
}
