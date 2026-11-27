package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Champion of Lambholt — Avacyn Restored #171.
 * {1}{G}{G} Creature — Human Warrior 1/1
 *
 * The evasion clause is the group-scoped [CantBeBlockedByCreaturesWithLessPower]: the threshold is
 * Champion's own projected power, checked for every attacking creature its controller controls
 * (Champion included) when blockers are declared.
 */
val ChampionOfLambholt = card("Champion of Lambholt") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "Creatures with power less than this creature's power can't block creatures you control.\n" +
        "Whenever another creature you control enters, put a +1/+1 counter on this creature."
    power = 1
    toughness = 1

    staticAbility {
        ability = CantBeBlockedByCreaturesWithLessPower(GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8ab9cd3-2faf-4500-a2ee-90b3a8d559c4.jpg?1783940670"
        ruling(
            "2012-05-01",
            "The comparison of power is performed only when blockers are declared. Increasing the " +
                "power of a blocking creature (or decreasing the power of Champion of Lambholt) after " +
                "this point won't cause any creature to stop blocking or become unblocked."
        )
    }
}
