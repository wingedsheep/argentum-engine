package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Tazri, Beacon of Unity — Zendikar Rising #44
 * {4}{W} · Legendary Creature — Human Warrior · 4/6
 *
 * This spell costs {1} less to cast for each creature in your party.
 * {2/U}{2/B}{2/R}{2/G}: Look at the top six cards of your library. You may reveal up to two Cleric,
 * Rogue, Warrior, Wizard, and/or Ally cards from among them and put them into your hand. Put the
 * rest on the bottom of your library in a random order.
 *
 * The discount is a self [ModifySpellCost] reading [DynamicAmounts.partySize] at cast time (it only
 * removes generic mana, so Tazri never costs less than {W}). The activation cost is four
 * monocolored hybrid symbols, each payable with {2} or one mana of its colour. The dig is the
 * look-at-top / `chooseUpToSplit` shape Reality Heist uses — up to two cards *total*.
 */
val TazriBeaconOfUnity = card("Tazri, Beacon of Unity") {
    manaCost = "{4}{W}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Human Warrior"
    oracleText = "This spell costs {1} less to cast for each creature in your party.\n" +
        "{2/U}{2/B}{2/R}{2/G}: Look at the top six cards of your library. You may reveal up to two " +
        "Cleric, Rogue, Warrior, Wizard, and/or Ally cards from among them and put them into your " +
        "hand. Put the rest on the bottom of your library in a random order."
    power = 4
    toughness = 6

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize())
            ),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2/U}{2/B}{2/R}{2/G}")
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (kept, rest) = chooseUpToSplit(
                2,
                from = looked,
                filter = GameObjectFilter.Any.withAnySubtype("Cleric", "Rogue", "Warrior", "Wizard", "Ally"),
                prompt = "You may reveal up to two Cleric, Rogue, Warrior, Wizard, and/or Ally cards " +
                    "to put into your hand.",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "44"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4b79138-5728-4541-9ea5-dce1a5ae7e6a.jpg"
        ruling(
            "2020-09-25",
            "To determine the cost of Tazri's activated ability, you choose which cost you'll pay for each " +
                "of the four mana symbols (either {2} or one mana of the appropriate color), add up the total " +
                "cost, apply any additional costs, then apply any cost reductions."
        )
        ruling(
            "2020-09-25",
            "Tazri's activated ability lets you put up to two cards total into your hand, not two cards of " +
                "each of the listed creature types."
        )
        ruling(
            "2020-09-25",
            "Ally is a creature type that doesn't appear in the Zendikar Rising set, but it has appeared in " +
                "previous sets featuring the plane of Zendikar."
        )
    }
}
