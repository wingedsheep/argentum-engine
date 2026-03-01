package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Savage Punch
 * {1}{G}
 * Sorcery
 * Target creature you control fights target creature you don't control.
 * Ferocious — The creature you control gets +2/+2 until end of turn before it fights
 * if you control a creature with power 4 or greater.
 */
val SavagePunch = card("Savage Punch") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"
    oracleText = "Target creature you control fights target creature you don't control.\nFerocious — The creature you control gets +2/+2 until end of turn before it fights if you control a creature with power 4 or greater."

    spell {
        val yourCreature = target("creature you control", TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.youControl())
        ))
        val theirCreature = target("creature you don't control", TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.opponentControls())
        ))
        // Ferocious: +2/+2 to your creature before fight if you control a creature with power 4+
        effect = ConditionalEffect(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            effect = Effects.ModifyStats(2, 2, yourCreature)
                .then(Effects.Fight(yourCreature, theirCreature)),
            elseEffect = Effects.Fight(yourCreature, theirCreature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c6b50dc-244a-4df5-a9a9-5a2b8f30d40c.jpg?1562788166"
    }
}
