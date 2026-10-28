package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * D'Vana Tendi, Optimistic Ensign
 * {1}{G/U}{G/U}
 * Legendary Creature — Orion Scientist
 * 2/4
 *
 * {T}: Untap another target permanent.
 * {3}: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val DVanaTendiOptimisticEnsign = card("D'Vana Tendi, Optimistic Ensign") {
    manaCost = "{1}{G/U}{G/U}"
    typeLine = "Legendary Creature — Orion Scientist"
    colorIdentity = "GU"
    oracleText = "{T}: Untap another target permanent.\n{3}: Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 2
    toughness = 4
    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetFilter.Permanent.other())
        effect = Effects.Untap(permanent)
    }
    activatedAbility {
        cost = Costs.Mana("{3}")
        effect = Effects.Surveil(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Micah Epstein"
        flavorText = "\"I got to hold a heart!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/206239ca-341a-4f07-8149-a24ccf20ee45.jpg?1792508323"
    }
}
