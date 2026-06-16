package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Obeka, Splitter of Seconds
 * {1}{U}{B}{R}
 * Legendary Creature — Ogre Warlock
 * 2/5
 *
 * Menace
 * Whenever Obeka deals combat damage to a player, you get that many additional upkeep
 * steps after this phase.
 *
 * Implementation:
 * - The combat-damage trigger reads "that many" as
 *   [DynamicAmount.ContextProperty]`(TRIGGER_DAMAGE_AMOUNT)` — the amount of combat damage
 *   Obeka just dealt (mirrors The Key to the Vault).
 * - "You get that many additional upkeep steps after this phase" →
 *   [Effects.AddAdditionalUpkeepSteps]. Per CR 500.10, each added upkeep step creates the
 *   beginning phase that normally contains it, with the untap and draw steps skipped; per
 *   CR 500.8 these phases are inserted after the current phase (and after any additional
 *   combat phases added to the same point). "At the beginning of upkeep" abilities trigger in
 *   each (CR 503.1a). The TurnManager drains the queued count after the postcombat main phase.
 */
val ObekaSplitterOfSeconds = card("Obeka, Splitter of Seconds") {
    manaCost = "{1}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Ogre Warlock"
    power = 2
    toughness = 5
    oracleText = "Menace\n" +
        "Whenever Obeka deals combat damage to a player, you get that many additional upkeep " +
        "steps after this phase."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            binding = TriggerBinding.SELF
        )
        effect = Effects.AddAdditionalUpkeepSteps(
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Ryan Pancoast"
        flavorText = "\"You're living on borrowed time.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03415c42-086e-4a2e-9be8-5cdcde83f134.jpg?1712356168"
    }
}
