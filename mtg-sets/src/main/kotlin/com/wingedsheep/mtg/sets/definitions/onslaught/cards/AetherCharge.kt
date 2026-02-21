package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Aether Charge
 * {4}{R}
 * Enchantment
 * Whenever a Beast you control enters the battlefield, you may have it deal 4 damage
 * to target opponent.
 */
val AetherCharge = card("Aether Charge") {
    manaCost = "{4}{R}"
    typeLine = "Enchantment"
    oracleText = "Whenever a Beast enters the battlefield under your control, you may have it deal 4 damage to target opponent."

    triggeredAbility {
        trigger = TriggerSpec(
                ZoneChangeEvent(filter = GameObjectFilter.Creature.withSubtype(Subtype("Beast")).youControl(), to = Zone.BATTLEFIELD),
                TriggerBinding.ANY
            )
        val t = target("target", Targets.Opponent)
        effect = MayEffect(
            DealDamageEffect(4, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Mark Brill"
        flavorText = "\"Is it just me, or does that meteor have teeth?\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05df2792-4971-49e8-a8f2-17700e247500.jpg?1562896370"
    }
}
