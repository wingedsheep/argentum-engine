package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quicksmith Spy
 * {3}{U}
 * Creature — Human Artificer
 * 2/3
 * When this creature enters, target artifact you control gains "{T}: Draw a card" for as long as
 * you control this creature.
 *
 * The grant records its controller, so `EndedDurationExpiryCheck` removes it the moment the Spy
 * leaves the battlefield or changes control — and regaining the Spy does not restore it
 * (CR 611.2b).
 */
val QuicksmithSpy = card("Quicksmith Spy") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, target artifact you control gains \"{T}: Draw a card\" " +
        "for as long as you control this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val artifact = target(TargetFilter.Artifact.youControl())
        effect = Effects.GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = AbilityCost.Tap,
                effect = Effects.DrawCards(1)
            ),
            target = artifact,
            duration = Duration.WhileYouControlSource("this creature")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Ryan Alexander Lee"
        flavorText = "\"Let's just broaden its surveillance lens.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f922e87-1744-4966-9e3a-54917f7f3d9e.jpg?1783936769"
    }
}
