package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Ogre Slumlord
 * {3}{B}{B}
 * Creature — Ogre Rogue
 * 3/3
 * Whenever another nontoken creature dies, you may create a 1/1 black Rat creature token.
 * Rats you control have deathtouch.
 *
 * The dies trigger is Harvester of Souls' `Triggers.another(nontoken creature).dies()` with an
 * optional token mint. "Rats you control" is the bare tribal noun — a permanent filter — in a
 * Goblin Warchief-style [GrantKeyword] static.
 */
val OgreSlumlord = card("Ogre Slumlord") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Ogre Rogue"
    power = 3
    toughness = 3
    oracleText = "Whenever another nontoken creature dies, you may create a 1/1 black Rat creature token.\n" +
        "Rats you control have deathtouch."

    triggeredAbility {
        trigger = Triggers.another(
            GameObjectFilter(cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsNontoken))
        ).dies()
        effect = Effects.May(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Rat"),
            )
        )
    }

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.DEATHTOUCH,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Rat").youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Trevor Claxton"
        flavorText = "\"His tenement is filled with the most vile, disgusting vermin. It's infested with rats, too.\"\n—Branko One-Ear"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29727bd1-9415-408a-99de-dd992e26e767.jpg?1783940128"
    }
}
