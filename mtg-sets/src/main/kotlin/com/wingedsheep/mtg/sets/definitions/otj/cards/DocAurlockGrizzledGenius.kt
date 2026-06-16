package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyPlotCost
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.PlotCostTarget
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Doc Aurlock, Grizzled Genius
 * {G}{U}
 * Legendary Creature — Bear Druid
 * 2/3
 *
 * Spells you cast from your graveyard or from exile cost {2} less to cast.
 * Plotting cards from your hand costs {2} less.
 *
 * The first half is a [ModifySpellCost] with the new [SpellCostTarget.YouCastFromZones]
 * (graveyard + exile), matched by the cost calculator only when the casting player controls Doc
 * Aurlock and the spell is being cast from one of those zones. The second half is a
 * [ModifyPlotCost] — Plot (CR 718) is a special action, not a spell, so it has its own dedicated
 * cost-reduction path (`PlotCostReducer`). Both reductions only touch generic mana and floor at
 * {0}.
 */
val DocAurlockGrizzledGenius = card("Doc Aurlock, Grizzled Genius") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Bear Druid"
    power = 2
    toughness = 3
    oracleText = "Spells you cast from your graveyard or from exile cost {2} less to cast.\n" +
        "Plotting cards from your hand costs {2} less."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCastFromZones(
                zones = setOf(Zone.GRAVEYARD, Zone.EXILE),
                filter = GameObjectFilter.Any,
            ),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    staticAbility {
        ability = ModifyPlotCost(
            target = PlotCostTarget.YouPlotFromHand,
            modification = CostModification.ReduceGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Jesper Ejsing"
        flavorText = "\"Learning stops only when you close your mind off to wonder.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fc27b30-8c8e-434c-a72c-e1d409efc1ae.jpg?1712356080"

        ruling("2024-04-12", "The cost reduction applies to the total cost of the spell, including " +
            "additional costs such as kicker. It can't reduce a spell's cost below the colored mana " +
            "in its cost.")
        ruling("2024-04-12", "Plotting a card is a special action, not casting a spell. The plot " +
            "cost reduction applies only to the cost to plot, and the spell-cast reduction applies " +
            "only when you actually cast a spell.")
    }
}
