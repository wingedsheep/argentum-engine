package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Roalesk, Apex Hybrid
 * {2}{G}{G}{U}
 * Legendary Creature — Human Mutant
 * 4/5
 * Flying, trample
 * When Roalesk enters, put two +1/+1 counters on another target creature you control.
 * When Roalesk dies, proliferate, then proliferate again.
 */
val RoaleskApexHybrid = card("Roalesk, Apex Hybrid") {
    manaCost = "{2}{G}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Human Mutant"
    power = 4
    toughness = 5
    oracleText = "Flying, trample\n" +
        "When Roalesk enters, put two +1/+1 counters on another target creature you control.\n" +
        "When Roalesk dies, proliferate, then proliferate again. (Choose any number of permanents " +
        "and/or players, then give each another counter of each kind already there. Then do it again.)"

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Proliferate() then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "213"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de14b4fd-fb4e-4986-9123-69ae68b5050a.jpg?1783933389"
        ruling(
            "2019-05-03",
            "You proliferate twice all while Roalesk's last ability is resolving. Nothing can happen between the two proliferations, and no player may choose to take actions."
        )
    }
}
