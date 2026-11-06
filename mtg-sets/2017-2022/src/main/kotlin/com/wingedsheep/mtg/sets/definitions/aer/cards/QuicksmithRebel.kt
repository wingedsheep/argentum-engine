package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quicksmith Rebel
 * {3}{R}
 * Creature — Human Artificer
 * 3/2
 * When this creature enters, target artifact you control gains "{T}: This artifact deals 2 damage
 * to any target" for as long as you control this creature.
 *
 * Same shape as Quicksmith Spy: the grant records its controller, so it ends the moment the Rebel
 * leaves the battlefield or changes control. The granted ability's damage source is the artifact.
 */
val QuicksmithRebel = card("Quicksmith Rebel") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Human Artificer"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, target artifact you control gains \"{T}: This artifact " +
        "deals 2 damage to any target\" for as long as you control this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val artifact = target(TargetFilter.Artifact.youControl())
        effect = Effects.GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(2, anyTarget)
                description = "{T}: This artifact deals 2 damage to any target"
            },
            target = artifact,
            duration = Duration.WhileYouControlSource("this creature")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Kieran Yanner"
        flavorText = "\"Let's add just a bit more stopping power.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d5f9755-b137-439c-9b54-1ad71ec1f9ec.jpg?1783936750"
    }
}
