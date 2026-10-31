package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Masterful Replication — Core Set 2020 #65 (canonical printing; reprinted in MOC)
 * {5}{U} Instant
 *
 * "Choose one —
 *  • Create two 3/3 colorless Golem artifact creature tokens.
 *  • Choose target artifact you control. Each other artifact you control becomes a copy of that
 *    artifact until end of turn."
 *
 * Mode 2 is Echoing Equation's shape over artifacts: [Effects.EachPermanentBecomesCopyOfTarget]
 * with `excludeTarget = true` and `Duration.EndOfTurn`. Copiable values only (CR 707.2) — counters
 * and non-copy effects on the target aren't copied, a target that is itself copying something
 * passes that on, and tokens stay tokens. Only artifacts you control as it resolves are affected.
 */
val MasterfulReplication = card("Masterful Replication") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Create two 3/3 colorless Golem artifact creature tokens.\n" +
        "• Choose target artifact you control. Each other artifact you control becomes a copy of " +
        "that artifact until end of turn."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Create two 3/3 colorless Golem artifact creature tokens") {
                effect = Effects.CreateToken(
                    power = 3,
                    toughness = 3,
                    creatureTypes = setOf("Golem"),
                    count = 2,
                    artifactToken = true,
                )
            },
            mode("Choose target artifact you control. Each other artifact you control becomes a copy of that artifact until end of turn") {
                val artifact = target(TargetFilter(GameObjectFilter.Artifact.youControl()))
                effect = Effects.EachPermanentBecomesCopyOfTarget(
                    target = artifact,
                    filter = GroupFilter(GameObjectFilter.Artifact.youControl()),
                    duration = Duration.EndOfTurn,
                    excludeTarget = true,
                )
            },
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63832647-bb4d-4380-8acb-cf11e8727672.jpg?1783933007"
        ruling("2019-07-12", "Masterful Replication copies the printed values of the target artifact. It won't copy counters on that artifact or effects that have changed its power, toughness, types, color, or so on. Notably, it won't copy effects that made the target artifact become an artifact creature if it isn't normally an artifact creature.")
        ruling("2019-07-12", "An Equipment that becomes a copy of a creature becomes unattached if it was attached to a creature. An Equipment that becomes a copy of another Equipment remains attached if it was attached to a creature.")
        ruling("2019-07-12", "If the target artifact is copying something else, then your artifacts become copies of whatever the target artifact copied.")
        ruling("2019-07-12", "Becoming a copy won't change whether an artifact is a token.")
    }
}
