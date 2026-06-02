package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Wingspan Stride
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has flying.
 * {2}{U}: Return this Aura to its owner's hand.
 */
val WingspanStride = card("Wingspan Stride") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has flying.\n" +
        "{2}{U}: Return this Aura to its owner's hand."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        description = "{2}{U}: Return this Aura to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Jake Murray"
        ruling(
            "2025-04-04",
            "If the creature this Aura is attached to leaves the battlefield or stops being a creature " +
                "before the activated ability resolves, Wingspan Stride will be put into its owner's " +
                "graveyard as a state-based action before that ability resolves, and it won't be returned " +
                "to its owner's hand."
        )
        imageUri = "https://cards.scryfall.io/normal/front/3/3/339a0e24-c332-4558-bb60-f5504ddde88c.jpg?1743204222"
    }
}
