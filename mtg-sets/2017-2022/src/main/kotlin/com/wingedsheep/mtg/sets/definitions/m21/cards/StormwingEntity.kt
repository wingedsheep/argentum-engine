package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Stormwing Entity
 * {3}{U}{U}
 * Creature — Elemental
 * 3/3
 * This spell costs {2}{U} less to cast if you've cast an instant or sorcery spell this turn.
 * Flying
 * Prowess
 * When this creature enters, scry 2.
 *
 * The {2}{U} discount is split into its generic and colored halves (Brush Off's shape), each a
 * self-cast [ModifySpellCost] gated on the same Mage Duel condition —
 * `YouCastSpellsThisTurn(1, InstantOrSorcery)`, read at cast time — so both apply together and only
 * once however many instants and sorceries were cast. Turn history: a countered spell still counts.
 */
val StormwingEntity = card("Stormwing Entity") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 3
    oracleText = "This spell costs {2}{U} less to cast if you've cast an instant or sorcery spell this turn.\n" +
        "Flying\n" +
        "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "When this creature enters, scry 2."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(
                Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = GameObjectFilter.InstantOrSorcery)
            ),
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceColored("{U}"),
            gating = CostGating.OnlyIf(
                Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = GameObjectFilter.InstantOrSorcery)
            ),
        )
    }

    keywords(Keyword.FLYING)
    prowess()

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0540ee72-6370-4f70-9526-6f441b3cac1e.jpg?1783930718"
        ruling("2020-06-23", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Stormwing Entity). The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2020-06-23", "Stormwing Entity's cost reduction applies even if the instant or sorcery spell you cast was countered or didn't resolve.")
        ruling("2020-06-23", "Stormwing Entity's cost reduction applies just once, no matter how many instant and sorcery spells you've cast this turn.")
    }
}
