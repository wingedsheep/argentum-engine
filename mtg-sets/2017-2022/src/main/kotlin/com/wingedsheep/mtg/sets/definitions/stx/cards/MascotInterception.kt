package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mascot Interception — Strixhaven: School of Mages #110 (canonical printing)
 * {3}{R} · Sorcery
 *
 * This spell costs {3} less to cast if it targets a creature token.
 * Gain control of target creature until end of turn. Untap that creature. It gets +2/+0 and
 * gains haste until end of turn.
 *
 * The discount is the Bury in Books shape: [SpellCostTarget.SelfCast] over
 * [CostReductionSource.FixedIfAnyTargetMatches] keyed on a creature-token permanent, so the {3}
 * comes off the generic part only (the {R} stands) and mana value stays 4. The spell is the
 * Threaten shape — [Effects.GainControl] until end of turn, [Effects.Untap], then
 * [Effects.ModifyStats] +2/+0 and [Effects.GrantKeyword] haste, both until end of turn.
 */
val MascotInterception = card("Mascot Interception") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell costs {3} less to cast if it targets a creature token.\n" +
        "Gain control of target creature until end of turn. Untap that creature. It gets +2/+0 " +
        "and gains haste until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.ModifyStats(2, 0, creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 3,
                    filter = GameObjectFilter.Creature.token().onBattlefield(),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Bryan Sola"
        flavorText = "Far from the trappings of royalty, Rowan and Will found inspiration from the most popular sport on campus, Mage Tower."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f0ea66f-1756-4b0d-8ccc-3b3a7370b531.jpg?1783927351"
        ruling("2021-04-16", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Mascot Interception). Mascot Interception's mana value is always 4, no matter what the total cost to cast it was.")
    }
}
