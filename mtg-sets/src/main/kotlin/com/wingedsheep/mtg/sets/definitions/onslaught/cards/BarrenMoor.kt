package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AddManaEffect
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Barren Moor
 * Land
 * Barren Moor enters the battlefield tapped.
 * {T}: Add {B}.
 * Cycling {B}
 */
val BarrenMoor = card("Barren Moor") {
    typeLine = "Land"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    keywordAbility(KeywordAbility.cycling("{B}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "312"
        artist = "Don Hazeltine"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb236caa-dbf0-4c75-9a56-e15866e7681b.jpg?1562929730"
    }
}
