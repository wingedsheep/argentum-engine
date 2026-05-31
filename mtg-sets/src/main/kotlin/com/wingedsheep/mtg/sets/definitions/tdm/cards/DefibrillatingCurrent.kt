package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect

/**
 * Defibrillating Current
 * {2/R}{2/W}{2/B}
 * Sorcery
 *
 * Defibrillating Current deals 4 damage to target creature or planeswalker and you gain 2 life.
 */
val DefibrillatingCurrent = card("Defibrillating Current") {
    manaCost = "{2/R}{2/W}{2/B}"
    colorIdentity = "RWB"
    typeLine = "Sorcery"
    oracleText = "Defibrillating Current deals 4 damage to target creature or planeswalker and you gain 2 life."

    spell {
        val t = target("target creature or planeswalker", Targets.CreatureOrPlaneswalker)
        effect = CompositeEffect(
            listOf(
                Effects.DealDamage(4, t),
                Effects.GainLife(2)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Isis"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf3a18cf-03db-4eb0-8d53-0c1a71e184da.jpg?1743204687"
    }
}
