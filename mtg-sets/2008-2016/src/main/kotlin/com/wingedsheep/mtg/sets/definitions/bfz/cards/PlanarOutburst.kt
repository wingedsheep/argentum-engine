package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Planar Outburst
 * {3}{W}{W}
 * Sorcery
 * Destroy all nonland creatures.
 * Awaken 4—{5}{W}{W}{W} (If you cast this spell for {5}{W}{W}{W}, also put four +1/+1 counters on
 * target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * Land creatures survive the sweep. The awaken ability runs after the destroy, so the awakened land
 * becomes a creature only once the sweep is done — and it is a land creature anyway.
 */
val PlanarOutburst = card("Planar Outburst") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all nonland creatures.\n" +
        "Awaken 4—{5}{W}{W}{W} (If you cast this spell for {5}{W}{W}{W}, also put four +1/+1 counters on " +
        "target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(4, "{5}{W}{W}{W}"))

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature and GameObjectFilter.Nonland)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f34f930-a7c6-400d-b6e8-b9908e0f0404.jpg?1783938216"
        ruling("2015-08-25", "A \"nonland creature\" is a creature that isn't also a land.")
        ruling(
            "2015-08-25",
            "If the non-awaken part of the spell doesn't require a target and you cast the spell for its awaken cost, then the spell won't resolve if the target land you control becomes illegal before the spell resolves (such as due to being destroyed in response to the spell being cast).",
        )
    }
}
