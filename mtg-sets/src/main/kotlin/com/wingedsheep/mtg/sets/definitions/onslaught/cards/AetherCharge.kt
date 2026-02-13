package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.OnCreatureWithSubtypeEnters

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
        trigger = OnCreatureWithSubtypeEnters(Subtype("Beast"), youControlOnly = true)
        target = Targets.Opponent
        effect = MayEffect(
            DealDamageEffect(4, EffectTarget.ContextTarget(0))
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
