package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Moopsy
 * {G}
 * Creature — Beast
 * 1/1
 *
 * Deathtouch
 * {2}: This creature gains reach until end of turn.
 */
val Moopsy = card("Moopsy") {
    manaCost = "{G}"
    typeLine = "Creature — Beast"
    colorIdentity = "G"
    oracleText = "Deathtouch\n{2}: This creature gains reach until end of turn."
    power = 1
    toughness = 1
    keywords(Keyword.DEATHTOUCH)
    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.GrantKeyword(Keyword.REACH, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "207"
        artist = "Brooklyn Smith"
        flavorText = "\"The moopsy is a small mammal known for its endearing appearance. The creature's diet consists of living bone tissue, which it liquefies and drinks via a process witnesses describe as 'upsettingly swift and graphic.'\"\n—*Lawmim Galactopedia*"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfbaf597-6be2-4aa0-88ae-cc4c94878add.jpg?1785981590"
    }
}

