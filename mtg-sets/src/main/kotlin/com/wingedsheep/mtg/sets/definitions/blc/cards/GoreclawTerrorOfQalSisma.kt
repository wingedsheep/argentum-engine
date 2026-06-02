package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Goreclaw, Terror of Qal Sisma {3}{G}
 * Legendary Creature — Bear
 * 4/3
 *
 * Creature spells you cast with power 4 or greater cost {2} less to cast.
 * Whenever Goreclaw attacks, each creature you control with power 4 or
 * greater gets +1/+1 and gains trample until end of turn.
 */
val GoreclawTerrorOfQalSisma = card("Goreclaw, Terror of Qal Sisma") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Bear"
    power = 4
    toughness = 3
    oracleText = "Creature spells you cast with power 4 or greater cost {2} less to cast.\n" +
        "Whenever Goreclaw, Terror of Qal Sisma attacks, each creature you control with " +
        "power 4 or greater gets +1/+1 and gains trample until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature.powerAtLeast(4)),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = GroupPatterns.modifyStatsForAll(
            power = 1,
            toughness = 1,
            filter = GroupFilter.AllCreaturesYouControl.powerAtLeast(4),
        ) then GroupPatterns.grantKeywordToAll(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter.AllCreaturesYouControl.powerAtLeast(4),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de8a64e5-9986-4692-b173-43475f4b5005.jpg?1721753137"
        flavorText = "You don't want to know how she got that name."
        ruling("2018-07-13", "If you cast a creature spell that will enter the battlefield with a number of +1/+1 counters, such as Hungering Hydra, those counters aren't considered when determining whether Goreclaw reduces that spell's cost. Similarly, effects that will raise the creature's power once it has entered the battlefield won't apply.")
        ruling("2018-07-13", "Goreclaw's last ability affects only creatures you control with the appropriate power at the time it resolves. Creatures you begin to control later in the turn won't get either bonus, and a creature you control whose power decreases later in the turn won't lose either bonus.")
    }
}
