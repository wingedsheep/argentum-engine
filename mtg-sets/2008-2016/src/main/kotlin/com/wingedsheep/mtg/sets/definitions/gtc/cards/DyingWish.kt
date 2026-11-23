package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dying Wish
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature you control
 * When enchanted creature dies, target player loses X life and you gain X life, where X is its power.
 *
 * Mask of Griselbrand's shape: an attached-bound dies trigger whose X is the triggering creature's
 * power read as last-known information off the zone change. The loss and the gain are two separate
 * X-sized effects (Blood Artist's shape), not a drain of the life actually lost.
 */
val DyingWish = card("Dying Wish") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When enchanted creature dies, target player loses X life and you gain X life, where X is its power."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        val player = target(Targets.Player)
        effect = Effects.LoseLife(DynamicAmounts.triggeringPower(), player) then
            Effects.GainLife(DynamicAmounts.triggeringPower())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Scott Chou"
        flavorText = "\"I wish I could rewrite my every deed and kill you with my first breath instead of my last.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b46e83d3-c66d-42fb-8435-b6c448db01ae.jpg?1783940131"

        ruling(
            "2013-01-24",
            "Use the creature's power the last time it was on the battlefield to determine how much life the target player loses and how much life you gain."
        )
        ruling(
            "2013-01-24",
            "If another player gains control of either Dying Wish or the enchanted creature (but not both), Dying Wish will be enchanting an illegal permanent. The Aura will be put into its owner's graveyard as a state-based action."
        )
    }
}
