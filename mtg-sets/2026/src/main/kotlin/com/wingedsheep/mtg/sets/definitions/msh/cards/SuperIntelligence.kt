package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Super Intelligence
 * {U}
 * Enchantment — Aura
 *
 * Enchant creature
 * At the beginning of the upkeep of enchanted creature's controller, that player draws a card.
 *
 * The ATTACHED-binding step trigger (Lingering Death shape) fires only on the upkeep of the
 * *enchanted creature's* controller. The Aura's controller still controls the ability, so "that
 * player" is the triggering player the step binds, not the default
 * [com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller].
 */
val SuperIntelligence = card("Super Intelligence") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "At the beginning of the upkeep of enchanted creature's controller, that player draws a card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
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
