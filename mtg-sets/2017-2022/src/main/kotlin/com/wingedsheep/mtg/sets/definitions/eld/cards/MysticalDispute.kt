package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mystical Dispute — Throne of Eldraine #58 (canonical printing)
 * {2}{U} · Instant
 *
 * This spell costs {2} less to cast if it targets a blue spell.
 * Counter target spell unless its controller pays {3}.
 *
 * Out of Air's shape with Scatter Ray's tax counter: [Effects.CounterUnlessPays] on any
 * spell, plus a [SpellCostTarget.SelfCast] generic reduction over
 * [CostReductionSource.FixedIfAnyTargetMatches] keyed on a blue object (the chosen target is a
 * spell on the stack, matched by colour the way Gainsay's target is). Only the generic {2} comes
 * off — the {U} stands — and mana value stays 3.
 */
val MysticalDispute = card("Mystical Dispute") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if it targets a blue spell.\n" +
        "Counter target spell unless its controller pays {3}."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{3}")
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 2,
                    filter = GameObjectFilter.Any.withColor(Color.BLUE),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Ekaterina Burmak"
        flavorText = "\"Poor, lost mage. Your feet are on land, yet you're in over your head, aren't you?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbe04cb8-a8b9-4241-baae-b398a2509a3a.jpg?1783932655"
    }
}
