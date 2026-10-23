package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Devouring Tendrils
 * {1}{G}
 * Sorcery
 * Target creature you control deals damage equal to its power to target creature or
 * planeswalker you don't control. When the permanent you don't control dies this turn,
 * you gain 2 life.
 *
 * Composition: Clear Shot's "it deals damage equal to its power" bite, then a
 * watched-target delayed trigger (Reincarnation's shape) on the second target. Per the
 * ruling, the delayed trigger fires however that permanent dies this turn, not only from
 * this damage — so it is an entity-scoped `dies()` trigger, not a damage-linked one.
 */
val DevouringTendrils = card("Devouring Tendrils") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to target creature or " +
        "planeswalker you don't control. When the permanent you don't control dies this turn, you gain 2 life."

    spell {
        val yourCreature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        val theirPermanent = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.DealDamage(DynamicAmounts.powerOf(yourCreature), theirPermanent, damageSource = yourCreature) then
            Effects.CreateDelayedTrigger(
                effect = Effects.GainLife(2),
                trigger = Triggers.self.dies(),
                watchedTarget = theirPermanent,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Campbell White"
        flavorText = "Witherbloom spells often take on the appearance—and personality—of their components."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7444555-da19-4d2d-ad81-233c54fbb78e.jpg?1783927344"
        ruling("2021-04-16", "Devouring Tendrils creates a delayed triggered ability that triggers whenever the target creature or planeswalker you don't control dies this turn, even if it didn't die because of the damage dealt as Devouring Tendrils resolved.")
    }
}
