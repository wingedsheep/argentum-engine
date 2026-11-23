package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Alms Beast
 * {2}{W}{B}
 * Creature — Beast
 * 6/6
 * Creatures blocking or blocked by this creature have lifelink.
 *
 * A static grant over the live combat pairing with this creature (CR 509): its blockers, or the
 * attacker it blocks, have lifelink for as long as the pairing lasts.
 */
val AlmsBeast = card("Alms Beast") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Beast"
    power = 6
    toughness = 6
    oracleText = "Creatures blocking or blocked by this creature have lifelink."

    staticAbility {
        ability = GrantKeyword(
            Keyword.LIFELINK,
            GroupFilter(GameObjectFilter.Creature.blockingOrBlockedBySource())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Dan Murayama Scott"
        flavorText = "Alms-coins are only redeemable at Orzhov businesses."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce441759-cd4c-4bcc-925e-08e8b60853c0.jpg?1783940113"
    }
}
