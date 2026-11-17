package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dragonscale General — Fate Reforged #11 (canonical printing)
 * {3}{W} · Creature — Human Warrior · 2/3
 *
 * At the beginning of your end step, bolster X, where X is the number of tapped creatures you
 * control.
 *
 * X is counted as the trigger resolves (the dynamic bolster reads it when the counters are
 * placed), and the bolstered creature needn't be one of the tapped ones.
 */
val DragonscaleGeneral = card("Dragonscale General") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 3
    oracleText = "At the beginning of your end step, bolster X, where X is the number of tapped creatures " +
        "you control. (Choose a creature with the least toughness among creatures you control and put X " +
        "+1/+1 counters on it.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Patterns.Mechanic.bolster(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.tapped()).count()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Volkan Baǵa"
        flavorText = "\"Dragons seek war. I bring it to them.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df904cc6-3ee0-4326-a854-818695aa2890.jpg?1783938713"
        ruling("2014-11-24", "Count the number of tapped creatures you control as the ability resolves to determine the value of X.")
        ruling("2014-11-24", "The creature you put the counters on doesn't have to be one of the tapped creatures.")
        ruling("2014-11-24", "Bolster itself doesn't target any creature, though some spells and abilities that bolster may have other effects that target creatures. For example, you could put counters on a creature with protection from white with Abzan Skycaptain's bolster ability.")
        ruling("2014-11-24", "You determine which creature to put counters on as the spell or ability that instructs you to bolster resolves.")
    }
}
