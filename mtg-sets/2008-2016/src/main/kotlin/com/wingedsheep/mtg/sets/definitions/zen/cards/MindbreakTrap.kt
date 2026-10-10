package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mindbreak Trap
 * {2}{U}{U}
 * Instant — Trap
 *
 * If an opponent cast three or more spells this turn, you may pay {0} rather than pay this
 * spell's mana cost.
 * Exile any number of target spells.
 *
 * The trap condition is per opponent (ruling: three opponents casting one spell each won't do),
 * hence [Conditions.AnOpponentCastSpellsThisTurnAtLeast] rather than a summed count. Each target
 * is exiled in turn — `ForEachTarget` narrows the context to one spell at a time, which is the
 * spell `ExileTargetSpell` reads. Exiling isn't countering, so it hits uncounterable spells.
 */
val MindbreakTrap = card("Mindbreak Trap") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant — Trap"
    oracleText = "If an opponent cast three or more spells this turn, you may pay {0} rather than " +
        "pay this spell's mana cost.\nExile any number of target spells."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        condition = Conditions.AnOpponentCastSpellsThisTurnAtLeast(3)
    )

    spell {
        targets(TargetFilter.SpellOnStack, unlimited = true)
        effect = Effects.ForEachTarget(Effects.ExileTargetSpell())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "57"
        artist = "Christopher Moeller"
        flavorText = "\"Life is a maze. This is one of its dead ends.\"\n—Noyan Dar, Tazeem lullmage"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f51140b-6254-431a-8810-94307bfdfbbe.jpg?1783942162"
        ruling("2025-09-19", "Mindbreak Trap's alternative cost condition checks whether an opponent cast three or more spells this turn, not whether those spells have resolved.")
        ruling("2025-09-19", "For Mindbreak Trap's alternative cost to apply, a single opponent must cast three or more spells. Three opponents each casting a single spell won't work, for example.")
        ruling("2025-09-19", "If a spell is exiled, it's removed from the stack and thus will not resolve. The spell isn't countered; it just no longer exists. This works on spells that can't be countered.")
    }
}
