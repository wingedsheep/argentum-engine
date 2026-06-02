package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Kin-Tree Severance — Tarkir: Dragonstorm #200
 * {2/W}{2/B}{2/G} · Instant · Uncommon
 *
 * Exile target permanent with mana value 3 or greater.
 */
val KinTreeSeverance = card("Kin-Tree Severance") {
    manaCost = "{2/W}{2/B}{2/G}"
    colorIdentity = "WBG"
    typeLine = "Instant"
    oracleText = "Exile target permanent with mana value 3 or greater."

    spell {
        val t = target(
            "target permanent with mana value 3 or greater",
            TargetPermanent(filter = TargetFilter.Permanent.manaValueAtLeast(3))
        )
        effect = Effects.Move(t, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Zack Stella"
        flavorText = "\"For his crimes—burn his blood and cut him from his ancestors. Perennation is only for family.\"\n" +
            "—Dictate of House Mevak in the banishment of Oret"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b577e246-3377-42aa-856e-b9fa89f3603a.jpg?1743204786"
    }
}
