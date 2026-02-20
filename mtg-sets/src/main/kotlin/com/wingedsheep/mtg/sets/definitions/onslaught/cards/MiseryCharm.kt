package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Misery Charm
 * {B}
 * Instant
 * Choose one —
 * • Destroy target Cleric.
 * • Return target Cleric card from your graveyard to your hand.
 * • Target player loses 2 life.
 */
val MiseryCharm = card("Misery Charm") {
    manaCost = "{B}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Destroy target Cleric.\n• Return target Cleric card from your graveyard to your hand.\n• Target player loses 2 life."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target Cleric") {
                target = TargetCreature(filter = TargetFilter.Creature.withSubtype("Cleric"))
                effect = MoveToZoneEffect(
                    target = EffectTarget.ContextTarget(0),
                    destination = Zone.GRAVEYARD,
                    byDestruction = true
                )
            }
            mode("Return target Cleric card from your graveyard to your hand") {
                target = TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.Any.withSubtype("Cleric").ownedByYou(),
                        zone = Zone.GRAVEYARD
                    )
                )
                effect = MoveToZoneEffect(
                    target = EffectTarget.ContextTarget(0),
                    destination = Zone.HAND
                )
            }
            mode("Target player loses 2 life") {
                target = TargetPlayer()
                effect = LoseLifeEffect(2, EffectTarget.ContextTarget(0))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/large/front/2/b/2be66eaf-222b-4c40-a9fa-aad56b9218e0.jpg?1562905282"
    }
}
