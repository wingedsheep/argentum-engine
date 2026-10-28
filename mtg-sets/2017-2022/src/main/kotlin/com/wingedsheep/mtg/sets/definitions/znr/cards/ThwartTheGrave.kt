package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Thwart the Grave — Zendikar Rising #130
 * {4}{B}{B} · Sorcery
 *
 * This spell costs {1} less to cast for each creature in your party.
 * Return target creature card and up to one target Cleric, Rogue, Warrior, or Wizard creature card
 * from your graveyard to the battlefield.
 *
 * The discount is Deadly Alliance's self-cast [ModifySpellCost] over [DynamicAmounts.partySize].
 * Two separate instances of "target": the first is any creature card (per the ruling it needn't be a
 * party type), the second is optional and restricted to the four party types. If only one target is
 * still legal on resolution, that one still returns.
 */
val ThwartTheGrave = card("Thwart the Grave") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "This spell costs {1} less to cast for each creature in your party. (Your party consists of " +
        "up to one each of Cleric, Rogue, Warrior, and Wizard.)\n" +
        "Return target creature card and up to one target Cleric, Rogue, Warrior, or Wizard creature card " +
        "from your graveyard to the battlefield."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize()),
            ),
        )
    }

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        val partyMember = target(
            TargetFilter.CreatureInYourGraveyard.withAnySubtype("Cleric", "Rogue", "Warrior", "Wizard"),
            optional = true,
        )
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.Move(partyMember, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd037e51-9088-4145-8289-00a86fb72ca1.jpg?1783929367"
        ruling("2020-09-25", "Thwart the Grave's first target doesn't have to be one of the party creature types.")
        ruling(
            "2020-09-25",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying, add any cost increases, then apply any cost reductions. The mana value of the spell " +
                "is determined only by its mana cost, no matter what the total cost to cast the spell was."
        )
    }
}
