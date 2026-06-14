package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Boom Box
 * {2}
 * Artifact
 *
 * {6}, {T}, Sacrifice this artifact: Destroy up to one target artifact, up to one
 * target creature, and up to one target land.
 *
 * Each of the three "up to one target" requirements is its own optional prompt with its
 * own legal-target list, so the player may choose any subset (including none) and any
 * combination of the three types. Rather than bind each chosen target positionally, the
 * effect gathers every chosen target ([CardSource.ChosenTargets]) into one collection and
 * destroys them — mirroring Pull Through the Weft, which keeps multi-optional-target
 * routing correct even when the per-requirement chosen counts differ.
 */
val BoomBox = card("Boom Box") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{6}, {T}, Sacrifice this artifact: Destroy up to one target artifact, " +
        "up to one target creature, and up to one target land."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{6}"),
            Costs.Tap,
            Costs.SacrificeSelf
        )
        target(
            "up to one target artifact",
            TargetObject(optional = true, filter = TargetFilter.Artifact)
        )
        target(
            "up to one target creature",
            TargetObject(optional = true, filter = TargetFilter.Creature)
        )
        target(
            "up to one target land",
            TargetObject(optional = true, filter = TargetFilter.Land)
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            destroy(chosen)
        }
        description = "{6}, {T}, Sacrifice this artifact: Destroy up to one target artifact, " +
            "up to one target creature, and up to one target land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Caio Monteiro"
        flavorText = "The cruel irony of explosives is they're most interesting exactly when " +
            "you should be running away as fast as you can."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea61d964-6d73-422e-9e08-360ac66f237a.jpg?1712356258"
    }
}
