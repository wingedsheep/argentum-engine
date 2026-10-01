package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gleeful Demolition
 * {R}
 * Sorcery
 *
 * Destroy target artifact. If you controlled that artifact, create three 1/1 red Phyrexian Goblin
 * creature tokens.
 *
 * "If you controlled that artifact" reads control as of resolution, before the destroy — the
 * `ControllerComponent` is gone once the artifact leaves the battlefield. The pipeline snapshots
 * whether you control the target with `filterSplit` first, then destroys it. The token clause is
 * keyed to control only, not to "destroyed this way", so an indestructible artifact you control
 * still yields the Goblins. An illegal target on resolution counters the spell and nothing happens.
 */
val GleefulDemolition = card("Gleeful Demolition") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact. If you controlled that artifact, create three 1/1 red " +
        "Phyrexian Goblin creature tokens."

    spell {
        target(TargetFilter.Artifact)
        effect = Effects.Pipeline(
            descriptionOverride = "Destroy target artifact. If you controlled that artifact, " +
                "create three 1/1 red Phyrexian Goblin creature tokens."
        ) {
            val targeted = gather(CardSource.ChosenTargets)

            // "you controlled that artifact" — snapshotted while it is still on the battlefield.
            val (yours, _) = filterSplit(targeted, GameObjectFilter.Any.youControl())

            moveTracked(
                targeted,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )

            ifNotEmpty(yours) {
                run(
                    Effects.CreateToken(
                        count = 3,
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.RED),
                        creatureTypes = setOf("Phyrexian", "Goblin"),
                        imageUri = "https://cards.scryfall.io/normal/front/3/6/3663e79b-2bf9-44af-a638-c0ad9067d8d4.jpg?1783918169",
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Tuan Duong Chu"
        flavorText = "Urabrask encourages creativity in all its forms, even the destructive ones. " +
            "Especially the destructive ones."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3e4efa6-5783-4a51-99c6-116d1a8f01cf.jpg?1783918030"
    }
}
