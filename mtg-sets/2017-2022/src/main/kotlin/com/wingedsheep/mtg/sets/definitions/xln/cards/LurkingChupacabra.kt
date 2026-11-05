package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lurking Chupacabra
 * {3}{B}
 * Creature — Beast Horror
 * 2/3
 *
 * Whenever a creature you control explores, target creature an opponent controls gets -2/-2
 * until end of turn.
 */
val LurkingChupacabra = card("Lurking Chupacabra") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Beast Horror"
    oracleText = "Whenever a creature you control explores, target creature an opponent controls " +
        "gets -2/-2 until end of turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).explores()
        val victim = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        effect = Effects.ModifyStats(-2, -2, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "YW Tang"
        flavorText = "\"Shadow birthed it. The moon gave it eyes, and the jungle gave it teeth. " +
            "But it was Death that taught it cruelty.\"\n—Huatli"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abdbaa34-1ee5-4a2a-bdb3-2f04809a5b42.jpg?1783935759"
    }
}
