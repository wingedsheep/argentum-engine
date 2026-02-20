package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Elvish Aberration
 * {5}{G}
 * Creature — Elf Mutant
 * 4/5
 * {T}: Add {G}{G}{G}.
 * Forestcycling {2} ({2}, Discard this card: Search your library for a Forest card,
 * reveal it, put it into your hand, then shuffle.)
 */
val ElvishAberration = card("Elvish Aberration") {
    manaCost = "{5}{G}"
    typeLine = "Creature — Elf Mutant"
    power = 4
    toughness = 5
    oracleText = "{T}: Add {G}{G}{G}.\nForestcycling {2}"

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN, 3)
        manaAbility = true
    }

    keywordAbility(KeywordAbility.Typecycling("Forest", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Anthony S. Waters"
        flavorText = "\"It once tended the forest. Now it feeds on it.\"\n—Wirewood tracker"
        imageUri = "https://cards.scryfall.io/large/front/1/3/137d326f-83e1-449a-b934-71c7986c64e7.jpg?1562525888"
    }
}
