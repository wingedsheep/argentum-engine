package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kitsune Mystic // Autumn-Tail, Kitsune Sage (Champions of Kamigawa #28) — a flip card (CR 710).
 *
 * Kitsune Mystic {3}{W} — Creature — Fox Wizard 2/3
 * "At the beginning of the end step, if this creature is enchanted by two or more Auras, flip it."
 *
 * Autumn-Tail, Kitsune Sage — Legendary Creature — Fox Wizard 4/5
 * "{1}: Attach target Aura attached to a creature to another creature."
 *
 * "Another creature" is chosen as the ability resolves, not targeted, and only among creatures the
 * Aura could legally enchant.
 */
private val KitsuneMysticUpright = card("Kitsune Mystic") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Wizard"
    oracleText = "At the beginning of the end step, if this creature is enchanted by two or more Auras, flip it."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.aurasAttachedToSelf(),
            ComparisonOperator.GTE,
            2
        )
        effect = Effects.Flip()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Jim Murray"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2ddf1a3-e6fa-4dd0-b80d-1a585b51b934.jpg?1783944336"
    }
}

private val AutumnTailKitsuneSage = card("Autumn-Tail, Kitsune Sage") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Fox Wizard"
    oracleText = "{1}: Attach target Aura attached to a creature to another creature."
    power = 4
    toughness = 5

    activatedAbility {
        cost = Costs.Mana("{1}")
        val aura = target(
            TargetFilter(GameObjectFilter.Enchantment.withSubtype("Aura").attachedTo(GameObjectFilter.Creature))
        )
        effect = Effects.AttachToChosenHost(aura, GameObjectFilter.Creature)
        description = "{1}: Attach target Aura attached to a creature to another creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Jim Murray"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2ddf1a3-e6fa-4dd0-b80d-1a585b51b934.jpg?1783944336"
    }
}

val KitsuneMystic: CardDefinition = CardDefinition.flipCard(
    unflipped = KitsuneMysticUpright,
    flipped = AutumnTailKitsuneSage,
)
