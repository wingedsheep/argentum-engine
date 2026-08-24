package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wielding the Green Dragon
 * {1}{G}
 * Sorcery
 * Target creature gets +4/+4 until end of turn.
 */
val WieldingTheGreenDragon = card("Wielding the Green Dragon") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +4/+4 until end of turn."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(4, 4, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Quan Xuejun"
        flavorText = "Named for the eastern part of the sky, the source of energy and renewal, Guan Yu's crescent-moon blade weighed over 100 pounds."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b138167-8129-4109-a58b-af26c95577e4.jpg"
    }
}
