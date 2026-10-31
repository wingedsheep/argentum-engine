package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Retributive Wand (M20 #236)
 * {3}  Artifact
 *
 * {3}, {T}: This artifact deals 1 damage to any target.
 * When this artifact is put into a graveyard from the battlefield, it deals 5 damage to any target.
 *
 * The graveyard clause is the self "dies" trigger (`Triggers.self.dies()`): a noncreature
 * permanent put into a graveyard from the battlefield is the same zone-change event.
 */
val RetributiveWand = card("Retributive Wand") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T}: This artifact deals 1 damage to any target.\n" +
        "When this artifact is put into a graveyard from the battlefield, it deals 5 damage to any target."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(1, anyTarget)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(5, anyTarget)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Zezhou Chen"
        flavorText = "\"The last blast is the biggest.\"\n—Sargis Haz, artificer"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5521cb92-03b0-44ab-a141-40dc0eb5a79f.jpg"
    }
}
