package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Super Intelligence
 * {U}
 * Enchantment — Aura
 *
 * Enchant creature
 * At the beginning of the upkeep of enchanted creature's controller, that player draws a card.
 *
 * The ATTACHED-binding step trigger (Custody Battle / Lingering Death shape) fires only on the
 * upkeep of the *enchanted creature's* controller, and the resulting ability's controller is that
 * same player — so the default [com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller] on
 * the draw resolves to "that player", not to the Aura's controller.
 */
val SuperIntelligence = card("Super Intelligence") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "At the beginning of the upkeep of enchanted creature's controller, that player draws a card."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.phase(Step.UPKEEP, binding = TriggerBinding.ATTACHED)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Michele Giorgi"
        flavorText = "\"With my gamma-enhanced genius, I am always at least two-hundred steps ahead.\"\n" +
            "—Leader, Samuel Sterns"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f26613a1-b4ad-4c7f-8dd9-8f82de995f0c.jpg?1783902951"
    }
}
