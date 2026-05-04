package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect

/**
 * Molecular Modifier
 * {2}{R}
 * Creature — Kavu Artificer
 * At the beginning of combat on your turn, target creature you control gets +1/+0 and gains first strike until end of turn.
 * 2/2
 */
val MolecularModifier = card("Molecular Modifier") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Kavu Artificer"
    power = 2
    toughness = 2
    oracleText = "At the beginning of combat on your turn, target creature you control gets +1/+0 and gains first strike until end of turn."

    // At the beginning of combat on your turn, target creature you control gets +1/+0 and gains first strike until end of turn
    triggeredAbility {
        trigger = Triggers.BeginCombat
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = CompositeEffect(
            listOf(
                Effects.ModifyStats(+1, 0, creature),
                Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Konstantin Porubov"
        flavorText = "\"Let's show those bugs our own style of splicin'.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b80b9c9-a871-4c04-b8be-feb81a900591.jpg?1753683209"
    }
}
