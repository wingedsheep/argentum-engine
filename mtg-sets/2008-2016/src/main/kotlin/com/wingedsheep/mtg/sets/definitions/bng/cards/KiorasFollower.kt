package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kiora's Follower — Born of the Gods #150 (canonical printing)
 * {G}{U} · Creature — Merfolk · 2/2
 *
 * {T}: Untap another target permanent.
 */
val KiorasFollower = card("Kiora's Follower") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Merfolk"
    power = 2
    toughness = 2
    oracleText = "{T}: Untap another target permanent."

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetFilter.Permanent.copy(excludeSelf = true))
        effect = Effects.Untap(permanent)
        description = "Untap another target permanent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Eric Deschamps"
        flavorText = "\"She may call herself Kiora but I believe she is Thassa, the embodiment of the " +
            "sea and empress of the depths.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec9ce307-8ec3-4db6-a4a3-49f92ebcc783.jpg?1783939523"
    }
}
