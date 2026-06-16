package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thornfist Striker
 * {2}{G}
 * Creature — Elf Druid
 * 3/3
 *
 * Ward {1}
 * Infusion — Creatures you control get +1/+0 and have trample as long as you gained life this turn.
 *
 * "Infusion" is an ability word (flavor only, CR 207.2c) — the mechanic is a conditional static
 * lord gated on `Conditions.YouGainedLifeThisTurn`. It splits into two continuous static abilities
 * (the +1/+0 in Layer 7c, the trample grant in Layer 6), each wrapped in `ConditionalStaticAbility`
 * so both turn on/off together as life is gained or the turn ends.
 */
val ThornfistStriker = card("Thornfist Striker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 3
    toughness = 3
    oracleText = "Ward {1} (Whenever this creature becomes the target of a spell or ability an " +
        "opponent controls, counter it unless that player pays {1}.)\nInfusion — Creatures you " +
        "control get +1/+0 and have trample as long as you gained life this turn."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    // Infusion — +1/+0 to creatures you control while you've gained life this turn.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 0,
                filter = GroupFilter.AllCreaturesYouControl,
            ),
            condition = Conditions.YouGainedLifeThisTurn,
        )
    }

    // Infusion — and they have trample under the same condition.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(
                keyword = Keyword.TRAMPLE,
                filter = GroupFilter.AllCreaturesYouControl,
            ),
            condition = Conditions.YouGainedLifeThisTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Diana Franco"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6cb838e-a06a-46ae-a30f-e5192178c1cc.jpg?1775938123"
    }
}
