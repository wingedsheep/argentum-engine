package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

// Current Oracle uses Lizard in place of the printed Viashino creature type.
val ViashinoBranchrider = card("Viashino Branchrider") {
    manaCost = "{R}"
    colorIdentity = "RG"
    typeLine = "Creature — Lizard Warrior"
    power = 1
    toughness = 1
    oracleText = "Kicker {2}{G} (You may pay an additional {2}{G} as you cast this spell.)\n" +
        "Haste\n" +
        "If this creature was kicked, it enters with two +1/+1 counters on it.\n" +
        "{2}{R}: This creature gets +2/+0 until end of turn."

    keywordAbility(KeywordAbility.kicker("{2}{G}"))
    keywords(Keyword.HASTE)
    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true,
        condition = Conditions.WasKicked,
    ))
    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f08749e-05e0-4fb4-a4ae-35e7187584ab.jpg?1783921307"
    }
}
