package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Flurry of Wings — Alara Reborn #127 (canonical printing)
 * {G}{W}{U} · Instant
 *
 * Create X 1/1 white Bird Soldier creature tokens with flying, where X is the number of
 * attacking creatures.
 *
 * X counts every attacking creature, whoever controls it, as the spell resolves (Divine
 * Retribution's count); outside combat it is zero and the spell does nothing (rulings).
 */
val FlurryOfWings = card("Flurry of Wings") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Instant"
    oracleText = "Create X 1/1 white Bird Soldier creature tokens with flying, where X is the number of " +
        "attacking creatures."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird", "Soldier"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/b/7/b7b55dcf-ae63-4b84-8d39-80b5a6de3c1a.jpg?1783942409",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Matt Cavotta"
        flavorText = "\"We are born soldiers. The call to battle is a homecoming.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbabaf1d-0220-438d-8263-e23d8010fe24.jpg?1783942412"
        ruling(
            "2009-05-01",
            "Flurry of Wings counts the number of creatures that are attacking as it resolves. It doesn't " +
                "matter how many creatures were originally declared as attackers that turn. It also doesn't " +
                "matter who controls those creatures, or who or what they're attacking."
        )
        ruling(
            "2009-05-01",
            "If Flurry of Wings is cast before attackers have been declared or after combat ends, it won't do " +
                "anything."
        )
    }
}
