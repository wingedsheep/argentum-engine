package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unnatural Restoration
 * {1}{G}
 * Sorcery
 * Return target permanent card from your graveyard to your hand. Proliferate.
 */
val UnnaturalRestoration = card("Unnatural Restoration") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"
    oracleText = "Return target permanent card from your graveyard to your hand. Proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        val t = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.ReturnToHand(t) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Jeremy Wilson"
        flavorText = "\"If you don't reassemble me immediately, the Mother of Machines will notice my absence, " +
            "and this entire layer will face her wrath!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d4e6a91-59cc-4f7f-af25-16acabdafb1a.jpg?1783918006"
    }
}
