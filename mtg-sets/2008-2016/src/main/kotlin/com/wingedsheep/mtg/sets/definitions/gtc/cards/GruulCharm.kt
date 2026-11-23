package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gruul Charm
 * {R}{G}
 * Instant
 * Choose one —
 * • Creatures without flying can't block this turn.
 * • Gain control of all permanents you own.
 * • Gruul Charm deals 3 damage to each creature with flying.
 *
 * The first mode is a dynamic group restriction (like Seismic Stomp), so creatures that lose flying
 * or enter without flying later this turn can't block either. The second mode is a permanent
 * layer-2 control effect with a fresh timestamp, so it overrides earlier control-changing effects.
 */
val GruulCharm = card("Gruul Charm") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Creatures without flying can't block this turn.\n• Gain control of all permanents you own.\n• Gruul Charm deals 3 damage to each creature with flying."
    spell {
        modal(chooseCount = 1) {
            mode("Creatures without flying can't block this turn") {
                effect = Effects.CantBlockGroup(
                    GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))
                )
            }
            mode("Gain control of all permanents you own") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Permanent.ownedByYou()),
                    Effects.GainControl(EffectTarget.IterationEntity)
                )
            }
            mode("Gruul Charm deals 3 damage to each creature with flying") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)),
                    Effects.DealDamage(3, EffectTarget.IterationEntity)
                )
            }
        }
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Zoltan Boros"
        flavorText = "\"Not Gruul? Then die!\"\n—Borborygmos"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/9235afe5-0a6b-43c2-921c-18524cf032f1.jpg?1783940106"
        ruling(
            "2013-01-24",
            "If you choose the first mode, no creature without flying will be able to block that turn, including creatures that lose flying and creatures without flying that enter after Gruul Charm resolves."
        )
        ruling(
            "2013-01-24",
            "If you choose the second mode, Gruul Charm's effect will override any other control-changing effects on those permanents."
        )
        ruling("2013-01-24", "A token is owned by the player under whose control it entered.")
    }
}
