package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Word of Binding
 * {X}{B}{B}
 * Sorcery
 * Tap X target creatures.
 *
 * The chosen X clamps how many creatures may be targeted (`dynamicMaxCount = XValue`), the same
 * shape Khans of Tarkir's Icy Blast uses for the identical line.
 */
val WordOfBinding = card("Word of Binding") {
    manaCost = "{X}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Tap X target creatures."

    spell {
        target = TargetCreature(optional = true, dynamicMaxCount = DynamicAmount.XValue)
        effect = Effects.TapEachTarget()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Ron Spencer"
        flavorText = "\"That was the worst experience of my days, standing there helpless as they " +
            "killed my whole troop.\" —Maeveen O'Donagh, *Memoirs of a Soldier*"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee30efdb-f1f1-497f-80a6-ec961db67c1d.jpg?1783947937"
    }
}
