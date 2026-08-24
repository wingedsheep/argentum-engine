package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.LandControllerScope

/**
 * Fellwar Stone
 * {2}
 * Artifact
 * {T}: Add one mana of any color that a land an opponent controls could produce.
 *
 * The colour set is computed from the opponents' lands at the moment the ability resolves, which is
 * exactly what `ManaColorSet.LandsCouldProduce(OPPONENTS)` is for — an opponent playing a Swamp
 * widens the Stone the same turn, and it produces nothing at all against an opponent with no lands.
 *
 * "Could produce" is about the lands' mana abilities, not about what they have been tapped for, so
 * a tapped opponent land still counts.
 */
val FellwarStone = card("Fellwar Stone") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color that a land an opponent controls could produce."

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddManaOfColorLandsCouldProduce(LandControllerScope.OPPONENTS)
        description = "{T}: Add one mana of any color that a land an opponent controls could produce."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Quinton Hoover"
        flavorText = "\"What do you have that I cannot obtain?\" —Mairsil, called the Pretender"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc47e322-f8b8-4685-b035-fda0cc433e6b.jpg?1783947926"
    }
}
