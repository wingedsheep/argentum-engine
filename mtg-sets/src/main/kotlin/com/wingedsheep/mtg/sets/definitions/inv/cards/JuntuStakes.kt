package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Juntu Stakes
 * {2}
 * Artifact
 * Creatures with power 1 or less don't untap during their controllers' untap steps.
 */
val JuntuStakes = card("Juntu Stakes") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Creatures with power 1 or less don't untap during their controllers' untap steps."

    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.DOESNT_UNTAP.name,
            filter = GroupFilter(GameObjectFilter.Creature.powerAtMost(1))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "304"
        artist = "Mark Brill"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ab7cf53-f62d-47e1-af70-ab12be0d22e2.jpg?1562906885"
    }
}
