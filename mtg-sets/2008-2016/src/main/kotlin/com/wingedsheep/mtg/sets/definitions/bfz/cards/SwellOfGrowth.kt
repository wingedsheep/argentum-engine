package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Swell of Growth — Battle for Zendikar #191 (canonical printing)
 * {1}{G} · Instant
 *
 * Target creature gets +2/+2 until end of turn. You may put a land card from your hand onto the
 * battlefield.
 *
 * A pump followed by the Eureka Moment land drop: [Patterns.Hand.putFromHand] selects *up to* one
 * land, which is the "you may"; it is not playing a land, so it ignores land drops and timing.
 */
val SwellOfGrowth = card("Swell of Growth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. You may put a land card from your hand onto the battlefield."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t) then
            Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Magali Villeneuve"
        flavorText = "\"Nissa is not the only one with a deep connection to Zendikar's primal elements.\"\n—Kiora"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b06da6fe-57bf-437e-8589-7e57a5881801.jpg?1783938185"
        ruling("2015-08-25", "Putting a land onto the battlefield this way is not the same as playing a land. Swell of Growth doesn't affect how many lands you can play from your hand during your turn.")
    }
}
