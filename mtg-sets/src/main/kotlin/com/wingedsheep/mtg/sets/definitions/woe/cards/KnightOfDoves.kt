package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Knight of Doves
 * {2}{W}
 * Creature — Human Knight
 * 1/3
 *
 * Whenever an enchantment you control is put into a graveyard from the battlefield, create a 1/1
 * white Bird creature token with flying.
 *
 * Same trigger shape as Wicked Visitor: any enchantment you control hitting the graveyard from
 * the battlefield — destroyed, sacrificed, self-sacrificing (Hopeless Nightmare) or a Role token
 * falling off when replaced — so it uses the generic `leavesBattlefield` factory with an `ANY`
 * binding rather than a SELF-bound constant. The payoff is a 1/1 white flying Bird token.
 */
val KnightOfDoves = card("Knight of Doves") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 1
    toughness = 3
    oracleText = "Whenever an enchantment you control is put into a graveyard from the battlefield, " +
        "create a 1/1 white Bird creature token with flying."

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/b/4b0c59f8-b407-47e7-8885-8c968f4ceecf.jpg?1783914993"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Volkan Baǵa"
        flavorText = "So many birds, yet the air was still and empty of song. Part of Syr Damon knew " +
            "something wasn't right, but it was a far-off worry, unimportant and soon forgotten."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4f33617-ad19-4d42-ab94-6f21a7fb3dd4.jpg?1783915132"
    }
}
