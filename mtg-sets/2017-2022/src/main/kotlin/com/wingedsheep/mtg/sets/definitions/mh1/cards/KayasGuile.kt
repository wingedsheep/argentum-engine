package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kaya's Guile — Modern Horizons #205 (canonical printing)
 * {1}{W}{B} · Instant
 *
 * Choose two —
 * • Each opponent sacrifices a creature of their choice.
 * • Exile all opponents' graveyards.
 * • Create a 1/1 white and black Spirit creature token with flying.
 * • You gain 4 life.
 * Entwine {3} (Choose all if you pay the entwine cost.)
 *
 * Entwine (CR 702.42a) is "choose all modes instead of just the number specified, and pay an
 * additional {3}" — paid once, however many modes that adds. So this is [entwine] on the rail's
 * optional-additional-cost slot plus [entwineModal] with `chooseCount = 2`, whose legal mode counts
 * are exactly {2, 4}; escalate's per-extra-mode cost would charge {6} and a count range would admit
 * three modes. Modes resolve in written order (CR 702.42b), so an entwined cast's first mode can
 * put a creature into a graveyard the second mode then exiles.
 */
val KayasGuile = card("Kaya's Guile") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Choose two —\n" +
        "• Each opponent sacrifices a creature of their choice.\n" +
        "• Exile all opponents' graveyards.\n" +
        "• Create a 1/1 white and black Spirit creature token with flying.\n" +
        "• You gain 4 life.\n" +
        "Entwine {3} (Choose all if you pay the entwine cost.)"

    entwine("{3}")

    spell {
        entwineModal(chooseCount = 2) {
            mode(
                "Each opponent sacrifices a creature of their choice",
                Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent))
            )
            mode("Exile all opponents' graveyards", Effects.ExileOpponentsGraveyards())
            mode(
                "Create a 1/1 white and black Spirit creature token with flying",
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE, Color.BLACK),
                    creatureTypes = setOf("Spirit"),
                    keywords = setOf(Keyword.FLYING),
                    imageUri = "https://cards.scryfall.io/normal/front/c/f/cf24e9fa-7d7f-4693-a9ed-b8e725d0ad5c.jpg?1783933222",
                )
            )
            mode("You gain 4 life", Effects.GainLife(4))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/179bd74b-c458-49d6-90ac-6075454e43dd.jpg?1783933081"
        ruling("2019-06-14", "If Kaya's Guile isn't entwined, you must choose two different modes.")
        ruling(
            "2019-06-14",
            "Each chosen mode is performed in the order specified. Abilities that trigger while one mode is being " +
                "performed won't be put onto the stack until Kaya's Guile has finished resolving."
        )
        ruling(
            "2019-06-14",
            "A creature sacrificed for the first mode of Kaya's Guile will be exiled if it ends up in an opponent's " +
                "graveyard and the second mode was also chosen."
        )
        ruling(
            "2019-06-14",
            "As the first mode is performed, first the opponent whose turn it is (or, if it's not an opponent's turn, " +
                "the next opponent in turn order) chooses a creature they control, then each other opponent in turn " +
                "order does the same, knowing the choices made before them. Then all the chosen creatures are " +
                "sacrificed at the same time."
        )
    }
}
