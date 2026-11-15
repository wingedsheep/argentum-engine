package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Warbringer
 * {3}{R}
 * Creature — Orc Berserker
 * 3 / 3
 *
 * Dash costs you pay cost {2} less (as long as this creature is on the battlefield).
 * Dash {2}{R}
 *
 * The discount is a battlefield [ModifySpellCost] scoped to dash casts by `castUsing =
 * Keyword.DASH`, so it never touches a spell's mana cost. Battlefield statics only function on the
 * battlefield, which is the parenthetical: Warbringer's own dash cost is paid while it's in hand,
 * so it is never discounted by itself. Only generic mana comes off — a {1}{R} dash costs {R}.
 */
val Warbringer = card("Warbringer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Berserker"
    power = 3
    toughness = 3
    oracleText = "Dash costs you pay cost {2} less (as long as this creature is on the battlefield).\n" +
        "Dash {2}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGeneric(2),
            castUsing = Keyword.DASH,
        )
    }

    dash = "{2}{R}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed4951ef-2519-4511-b952-c835cd81def7.jpg?1783938583"
    }
}
