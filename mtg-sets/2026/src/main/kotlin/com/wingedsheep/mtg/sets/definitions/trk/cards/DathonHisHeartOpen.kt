package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dathon, His Heart Open
 * {G}{U}{R}
 * Legendary Creature — Tamarian Officer
 * 3/3
 * Whenever you cast an enchantment spell, draw a card. If it's a Saga spell, Dathon deals 2 damage
 * to each opponent.
 * {2}: Choose a counter on target enchantment you control. Put another counter of that kind on it.
 * Activate only as a sorcery.
 *
 *  - The Saga check follows the draw, so it is a resolution-time [Effects.If], not an intervening
 *    "if". [Conditions.TriggeringSpellMatches] reads the spell's cast characteristics, so the answer
 *    holds even if the Saga was countered before the trigger resolved.
 *  - The counter ability uses [Effects.AddCountersOfChosenKind]: the kind is picked on resolution
 *    among the kinds on the enchantment (no prompt with one kind, no-op with none).
 */
val DathonHisHeartOpen = card("Dathon, His Heart Open") {
    manaCost = "{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Tamarian Officer"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast an enchantment spell, draw a card. If it's a Saga spell, Dathon deals 2 " +
        "damage to each opponent.\n" +
        "{2}: Choose a counter on target enchantment you control. Put another counter of that kind on it. " +
        "Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Enchantment)
        effect = Effects.DrawCards(1) then
            Effects.If(
                Conditions.TriggeringSpellMatches(GameObjectFilter.Enchantment.withSubtype("Saga")),
                Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent)),
            )
        description = "Whenever you cast an enchantment spell, draw a card. If it's a Saga spell, Dathon " +
            "deals 2 damage to each opponent."
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        val enchantment = target(TargetFilter.Enchantment.youControl())
        effect = Effects.AddCountersOfChosenKind(enchantment)
        timing = TimingRule.SorcerySpeed
        description = "{2}: Choose a counter on target enchantment you control. Put another counter of that " +
            "kind on it. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Dmitry Burmak"
        flavorText = "\"Darmok and Jalad at Tanagra.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2fe4663-eaaf-42c3-90f9-07e68129352c.jpg?1793187171"
    }
}
