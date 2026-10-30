package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fortifying Provisions
 * {2}{W}
 * Enchantment
 *
 * Creatures you control get +0/+1.
 * When this enchantment enters, create a Food token.
 */
val FortifyingProvisions = card("Fortifying Provisions") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +0/+1.\n" +
        "When this enchantment enters, create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    staticAbility {
        ability = ModifyStats(0, 1, GroupFilter.AllCreaturesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Scott Murphy"
        flavorText = "\"To the king, to the Circle, and to the realm! And to not dying.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98ce113c-53da-4fab-b7c9-fc3df248b65e.jpg?1783932674"
    }
}
