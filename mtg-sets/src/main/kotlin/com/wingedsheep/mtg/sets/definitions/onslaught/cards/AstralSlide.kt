package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ExileUntilEndStepEffect
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.OnCycle

/**
 * Astral Slide
 * {2}{W}
 * Enchantment
 * Whenever a player cycles a card, you may exile target creature.
 * If you do, return that card to the battlefield under its owner's control
 * at the beginning of the next end step.
 */
val AstralSlide = card("Astral Slide") {
    manaCost = "{2}{W}"
    typeLine = "Enchantment"
    oracleText = "Whenever a player cycles a card, you may exile target creature. If you do, return that card to the battlefield under its owner's control at the beginning of the next end step."

    triggeredAbility {
        trigger = OnCycle(controllerOnly = false)
        target = Targets.Creature
        effect = MayEffect(ExileUntilEndStepEffect(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Ron Spears"
        flavorText = "\"The hum of the universe is never off-key.\"\n—Mystic elder"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d14993b6-ed8d-4b9b-b54c-2837b343a61e.jpg?1562944880"
    }
}
