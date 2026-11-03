package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Replicator
 * {2}
 * Artifact
 * When this artifact enters, create a Food token.
 * {2}, {T}: Create a tapped token that's a copy of target token you control.
 */
val Replicator = card("Replicator") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "{2}, {T}: Create a tapped token that's a copy of target token you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val token = target(TargetFilter(GameObjectFilter.Token.youControl()))
        effect = Effects.CreateTokenCopyOfTarget(token, tapped = true)
        description = "{2}, {T}: Create a tapped token that's a copy of target token you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        artist = "William Phifer"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae846f45-d87b-47d4-87f6-2d9aa19c3c40.jpg?1792508444"
    }
}
