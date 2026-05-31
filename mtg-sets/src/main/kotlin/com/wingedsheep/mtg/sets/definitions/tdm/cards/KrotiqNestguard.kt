package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Krotiq Nestguard — Tarkir: Dragonstorm #148
 * {2}{G} · Creature — Insect · 4/4
 *
 * Defender
 * {2}{G}: This creature can attack this turn as though it didn't have defender.
 */
val KrotiqNestguard = card("Krotiq Nestguard") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 4
    toughness = 4
    oracleText = "Defender\n" +
        "{2}{G}: This creature can attack this turn as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.CanAttackThisTurn()
        description = "{2}{G}: This creature can attack this turn as though it didn't have defender."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Loïc Canavaggia"
        flavorText = "The bushwhacker's machete glanced off the root with a sickening crack. Slowly, the entire undergrowth turned to face him."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5d0a9fb-1068-478d-a78c-6fd77cc313f0.jpg?1743204557"
    }
}
