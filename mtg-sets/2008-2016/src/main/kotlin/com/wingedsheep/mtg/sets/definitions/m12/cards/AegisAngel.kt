package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aegis Angel
 * {4}{W}{W}
 * Creature — Angel
 * 5/5
 *
 * Flying
 * When this creature enters, another target permanent gains indestructible for as long as you
 * control this creature.
 *
 * The grant floats with `Duration.WhileYouControlSource` (Kyodai's shape): it ends for good once
 * Aegis Angel leaves the battlefield or changes controller, even if control later returns.
 */
val AegisAngel = card("Aegis Angel") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 5
    oracleText = "Flying (This creature can't be blocked except by creatures with flying or reach.)\n" +
        "When this creature enters, another target permanent gains indestructible for as long as you control this creature. (Effects that say \"destroy\" don't destroy it. A creature with indestructible can't be destroyed by damage.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent.other())
        effect = Effects.GrantKeyword(
            Keyword.INDESTRUCTIBLE,
            t,
            Duration.WhileYouControlSource("Aegis Angel")
        )
        description = "When this creature enters, another target permanent gains indestructible for as long as you control this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "1"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cdc19da-21af-45e7-ad1f-fcacd84a8d89.jpg?1783941107"
        ruling("2013-07-01", "If another player gains control of Aegis Angel, the permanent will no longer have indestructible, even if you regain control of Aegis Angel.")
        ruling("2011-09-22", "If Aegis Angel leaves the battlefield, you no longer control it.")
        ruling("2011-09-22", "If Aegis Angel and the permanent would be destroyed simultaneously, only Aegis Angel will be destroyed.")
    }
}
