package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Village Survivors
 * {4}{G}
 * Creature — Human
 * 4/5
 * Vigilance
 * Fateful hour — As long as you have 5 or less life, other creatures you control have vigilance.
 *
 * Gavony Ironwright's fateful-hour shape with a keyword grant: [GrantKeyword] over
 * [GroupFilter.OtherCreaturesYouControl] gated by [Conditions.LifeAtMost] 5.
 */
val VillageSurvivors = card("Village Survivors") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human"
    power = 4
    toughness = 5
    oracleText = "Vigilance\n" +
        "Fateful hour — As long as you have 5 or less life, other creatures you control have vigilance."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.OtherCreaturesYouControl),
            condition = Conditions.LifeAtMost(5)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "David Rapoza"
        flavorText = "As their villages fell to werewolves, fleeing refugees discovered they too had remarkable survival instincts."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13c0e852-9966-4145-b7c3-ac957f729376.jpg?1783940800"
    }
}
