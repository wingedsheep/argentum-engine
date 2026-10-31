package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Everquill Phoenix
 * {2}{R}{R}
 * Creature — Phoenix
 * 4/4
 * Mutate {3}{R}
 * Flying
 * Whenever this creature mutates, create a red artifact token named Feather with
 * "{1}, Sacrifice this token: Return target Phoenix card from your graveyard to the battlefield tapped."
 *
 * The Feather token is the predefined `Feather` token (PredefinedTokens), which carries the
 * sacrifice-to-reanimate ability.
 */
val EverquillPhoenix = card("Everquill Phoenix") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 4
    toughness = 4
    oracleText = "Mutate {3}{R} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Flying\n" +
        "Whenever this creature mutates, create a red artifact token named Feather with \"{1}, Sacrifice this token: Return target Phoenix card from your graveyard to the battlefield tapped.\""

    keywordAbility(KeywordAbility.mutate("{3}{R}"))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.CreatePredefinedToken("Feather")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/3953157f-2deb-4ab7-b4b3-592ebb7c7a9c.jpg?1783931052"
    }
}
