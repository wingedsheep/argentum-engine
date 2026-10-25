package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Thryx, the Sudden Storm
 * {3}{U}{U}
 * Legendary Creature — Elemental Giant
 * 4/5
 * Flash
 * Flying
 * Spells you cast with mana value 5 or greater cost {1} less to cast and can't be countered.
 *
 * - The discount is a [ModifySpellCost] over spells you cast with mana value 5+ (X counts at its
 *   announced value, CR 202.3e).
 * - The "can't be countered" half is a [GrantCantBeCountered] over spells you control that were
 *   cast (`cast()`), so a copy you control isn't protected. It applies to such a spell already on
 *   the stack when Thryx enters, even though that spell got no discount (ruling).
 */
val ThryxTheSuddenStorm = card("Thryx, the Sudden Storm") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Elemental Giant"
    power = 4
    toughness = 5
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\nFlying\n" +
        "Spells you cast with mana value 5 or greater cost {1} less to cast and can't be countered."

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.manaValueAtLeast(5)),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    staticAbility {
        ability = GrantCantBeCountered(
            filter = GameObjectFilter.Any.manaValueAtLeast(5).youControl().cast()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Mathias Kollros"
        flavorText = "Even in their mountain fortresses, the Akroans have learned not to anger Thassa."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4238b089-c6d8-4e4b-b6a1-a0f89e3b4968.jpg?1783931574"
        ruling("2020-01-24", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Thryx). The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2020-01-24", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value.")
        ruling("2020-01-24", "Thryx's last ability doesn't apply to itself while it's a spell.")
        ruling("2020-01-24", "A spell or ability that counters spells can still target spells you control with mana value 5 or greater. When that spell or ability resolves, your spell won't be countered, but any additional effects of the countering spell or ability will still happen.")
        ruling("2020-01-24", "If Thryx enters the battlefield while a spell with mana value 5 or greater that you cast is on the stack, that spell can't be countered, even though you don't get the {1} discount.")
    }
}
