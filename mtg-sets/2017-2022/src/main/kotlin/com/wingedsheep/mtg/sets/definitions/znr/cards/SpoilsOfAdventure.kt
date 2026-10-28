package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Spoils of Adventure — Zendikar Rising #237
 * {4}{W}{U} · Instant
 *
 * This spell costs {1} less to cast for each creature in your party.
 * You gain 3 life and draw three cards.
 *
 * The same party-size self-cast [ModifySpellCost] as Sea Gate Colossus; a full party casts it for {W}{U}.
 */
val SpoilsOfAdventure = card("Spoils of Adventure") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast for each creature in your party. (Your party consists of " +
        "up to one each of Cleric, Rogue, Warrior, and Wizard.)\nYou gain 3 life and draw three cards."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize()),
            ),
        )
    }

    spell {
        effect = Effects.GainLife(3) then Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Zezhou Chen"
        flavorText = "Survival itself is no small reward."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b3b1ef0-a653-4adb-8112-82d4822446b4.jpg?1783929318"
        ruling(
            "2020-09-25",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying, add any cost increases, then apply any cost reductions. The mana value of the spell " +
                "is determined only by its mana cost, no matter what the total cost to cast the spell was."
        )
        ruling(
            "2020-09-25",
            "If a spell has a cost reduction based on the number of creatures in your party, no player may " +
                "attempt to change that number after you begin to cast the spell but before you pay the cost."
        )
    }
}
