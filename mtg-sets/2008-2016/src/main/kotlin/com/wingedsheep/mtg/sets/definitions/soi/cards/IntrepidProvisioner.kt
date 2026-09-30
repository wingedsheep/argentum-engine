package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Intrepid Provisioner
 * {3}{G}
 * Creature — Human Scout
 * 3/3
 * Trample
 * When this creature enters, another target Human you control gets +2/+2 until end of turn.
 */
val IntrepidProvisioner = card("Intrepid Provisioner") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "When this creature enters, another target Human you control gets +2/+2 until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        val target = target(TargetFilter(GameObjectFilter.Permanent.youControl().withSubtype("Human"), excludeSelf = true))
        trigger = Triggers.self.enters()
        effect = Effects.ModifyStats(2, 2, target)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Lius Lasahido"
        flavorText = "\"How do I know where aid is needed? Look around you.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da3ac63b-8386-4175-9555-84c54566f5b4.jpg?1782712010"
    }
}
