package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Fortune Teller's Talent {U}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * You may look at the top card of your library any time.
 * {3}{U}: Level 2
 * As long as you've cast a spell this turn, you may play cards from the top of your library.
 * {2}{U}: Level 3
 * Spells you cast from anywhere other than your hand cost {2} less to cast.
 *
 * "Play cards from the top" is [PlayLandsAndCastFilteredFromTopOfLibrary] with an any-spell filter —
 * not `PlayFromTopOfLibrary`, which also plays with the top card revealed. "Anywhere other than your
 * hand" is every zone a spell can be cast from except the hand.
 */
val FortuneTellersTalent = card("Fortune Teller's Talent") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "You may look at the top card of your library any time.\n" +
        "{3}{U}: Level 2\n" +
        "As long as you've cast a spell this turn, you may play cards from the top of your library.\n" +
        "{2}{U}: Level 3\n" +
        "Spells you cast from anywhere other than your hand cost {2} less to cast."

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    classLevel(2, "{3}{U}") {
        staticAbility {
            condition = Conditions.YouCastSpellsThisTurn(1)
            ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = GameObjectFilter.Any)
        }
    }

    classLevel(3, "{2}{U}") {
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCastFromZones(
                    setOf(Zone.LIBRARY, Zone.GRAVEYARD, Zone.EXILE, Zone.COMMAND)
                ),
                modification = CostModification.ReduceGeneric(2),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1d43877-20ab-4e84-a597-4b5e03a6bf90.jpg?1783910734"
        ruling("2024-07-26", "You must pay all costs and follow all timing rules for lands played and spells cast from the top of your library this way.")
        ruling("2024-07-26", "The cost reduction applies only to generic mana in the total cost of spells you cast from anywhere other than your hand.")
        ruling("2024-07-26", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
