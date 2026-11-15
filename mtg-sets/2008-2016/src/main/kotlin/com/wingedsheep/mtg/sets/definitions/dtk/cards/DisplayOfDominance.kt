package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Display of Dominance
 * {1}{G}
 * Instant
 *
 * Choose one —
 * • Destroy target blue or black noncreature permanent.
 * • Permanents you control can't be the targets of blue or black spells your opponents control
 *   this turn.
 *
 * The second mode changes the rules rather than granting the permanents an ability (the card's
 * ruling), so it is a [CardsInZonesCantBeTargeted] lock granted to the caster for the turn: it
 * covers permanents that come under their control later that turn, and abilities — even from blue
 * or black sources — may still target them. A blue or black spell already targeting one finds that
 * target illegal as it resolves.
 */
val DisplayOfDominance = card("Display of Dominance") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target blue or black noncreature permanent.\n" +
        "• Permanents you control can't be the targets of blue or black spells your opponents control this turn."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target blue or black noncreature permanent") {
                val permanent = target(
                    TargetFilter(GameObjectFilter.NoncreaturePermanent.withAnyColor(Color.BLUE, Color.BLACK))
                )
                effect = Effects.Destroy(permanent)
            }
            mode("Permanents you control can't be the targets of blue or black spells your opponents control this turn") {
                effect = Effects.GrantStaticAbility(
                    CardsInZonesCantBeTargeted(
                        zones = setOf(Zone.BATTLEFIELD),
                        filter = GameObjectFilter.Permanent.youControl(),
                        sourceController = Player.EachOpponent,
                        spellFilter = GameObjectFilter.Any.withAnyColor(Color.BLUE, Color.BLACK)
                    ),
                    EffectTarget.Controller
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65572ff2-281f-431d-b776-e8231f24d2c6.jpg?1783938581"

        ruling("2015-02-25", "If you choose the second mode, and if a permanent you control is being targeted by a spell when Display of Dominance resolves, nothing happens right away. When that spell would resolve, its color is checked. If it's blue or black, that permanent will be an illegal target for that spell and won't be affected by it. If all that spell's targets have become illegal by the time it would resolve, it's countered.")
        ruling("2015-02-25", "If you choose the second mode, Display of Dominance will affect any permanent you happen to control at any point during the rest of the turn, not just permanents you control as it resolves. That's because it doesn't grant an ability to those permanents; rather, it affects the game rules and states something that's now true about those permanents.")
        ruling("2015-02-25", "Permanents you control may be the targets of abilities from blue or black sources controlled by your opponents.")
    }
}
