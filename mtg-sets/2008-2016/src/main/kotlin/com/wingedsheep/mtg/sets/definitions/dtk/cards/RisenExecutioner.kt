package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Risen Executioner
 * {2}{B}{B}
 * Creature — Zombie Warrior
 * 4/3
 * This creature can't block.
 * Other Zombie creatures you control get +1/+1.
 * You may cast this creature from your graveyard if you pay {1} more to cast it for each other
 * creature card in your graveyard.
 */
val RisenExecutioner = card("Risen Executioner") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Warrior"
    power = 4
    toughness = 3
    oracleText = "This creature can't block.\n" +
        "Other Zombie creatures you control get +1/+1.\n" +
        "You may cast this creature from your graveyard if you pay {1} more to cast it for each other " +
        "creature card in your graveyard."

    staticAbility {
        ability = CantBlock()
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).youControl(),
                excludeSelf = true,
            ),
        )
    }

    // The tax belongs to this permission: it is priced only into a cast from the graveyard, and
    // "other" skips this card while it still sits there being priced.
    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            costModification = CostModification.IncreaseGenericBy(
                CostReductionSource.Dynamic(
                    DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature.notSourceItself())
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "116"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95ff5e62-b73e-4b85-b65f-5226e75c7f97.jpg?1783938594"
        ruling("2015-02-25", "Risen Executioner's last ability doesn't change when you can cast it.")
        ruling("2015-02-25", "Once you start to cast Risen Executioner from your graveyard, you'll finish casting it, including paying its cost, before any player receives priority. No one can respond in time to affect the cost you'll pay (perhaps by destroying one of your other creatures).")
    }
}
