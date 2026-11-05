package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Unesh, Criosphinx Sovereign
 * {4}{U}{U}
 * Legendary Creature — Sphinx
 * 4/4
 * Flying
 * Sphinx spells you cast cost {2} less to cast.
 * Whenever Unesh or another Sphinx you control enters, reveal the top four cards of your
 * library. An opponent separates those cards into two piles. Put one pile into your hand and
 * the other into your graveyard.
 */
val UneshCriosphinxSovereign = card("Unesh, Criosphinx Sovereign") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Sphinx"
    power = 4
    toughness = 4
    oracleText = "Flying\nSphinx spells you cast cost {2} less to cast.\nWhenever Unesh or another Sphinx you control enters, reveal the top four cards of your library. An opponent separates those cards into two piles. Put one pile into your hand and the other into your graveyard."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype(Subtype.SPHINX)),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.SPHINX).youControl()).enters()
        effect = Patterns.Library.factOrFiction(count = 4)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "52"
        artist = "Adrian Majkrzak"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdd1a8b2-f4ba-45af-9586-98a98834b8bc.jpg?1783936046"
        ruling("2017-07-14", "Unesh's cost-reduction ability doesn't reduce the cost to cast itself.")
        ruling("2017-07-14", "You (not your opponent) choose which pile to put into your hand and which to put into your graveyard.")
        ruling("2017-07-14", "A pile can have no cards in it. In this case, you'll choose whether to put all the revealed cards into your hand or into your graveyard.")
    }
}
