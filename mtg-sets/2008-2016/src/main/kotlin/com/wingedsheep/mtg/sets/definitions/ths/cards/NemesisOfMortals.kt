package com.wingedsheep.mtg.sets.definitions.ths.cards

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

/**
 * Nemesis of Mortals — Theros #163
 * {4}{G}{G} · Creature — Snake · 5/5
 *
 * This spell costs {1} less to cast for each creature card in your graveyard.
 * {7}{G}{G}: Monstrosity 5. This ability costs {1} less to activate for each creature card in
 * your graveyard.
 */
val NemesisOfMortals = card("Nemesis of Mortals") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake"
    power = 5
    toughness = 5
    oracleText = "This spell costs {1} less to cast for each creature card in your graveyard.\n" +
        "{7}{G}{G}: Monstrosity 5. This ability costs {1} less to activate for each creature card in your graveyard. " +
        "(If this creature isn't monstrous, put five +1/+1 counters on it and it becomes monstrous.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.Creature,
                    amountPerCard = 1
                )
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{7}{G}{G}")
        genericCostReduction = DynamicAmounts.creatureCardsInYourGraveyard()
        effect = Effects.Monstrosity(5)
        description = "{7}{G}{G}: Monstrosity 5. This ability costs {1} less to activate for each creature card in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4010889-1d7a-4db3-a27c-c39baa024765.jpg?1783939744"
        ruling("2013-09-15", "Neither the cost-reducing ability of Nemesis of Mortals nor the condition of its monstrosity ability can reduce the colored mana requirement of those costs.")
        ruling("2013-09-15", "Once you declare you're casting Nemesis of Mortals or activating its ability, it's too late for players to try to change the number of creature cards in your graveyard to affect the cost of doing so.")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
