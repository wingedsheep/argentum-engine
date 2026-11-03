package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rules of Acquisition
 * {3}{U}{U}
 * Sorcery
 *
 * Choose one —
 * • Follow the 3rd Rule — Gain control of target nonland permanent an opponent controls. That
 *   opponent draws three cards.
 * • Follow the 98th Rule — Target opponent gains control of target nonland permanent you control.
 *   If they do, you draw three cards.
 *
 * Modelling notes:
 * - Mode 1: "that opponent" is the permanent's controller *before* the steal, so it is recorded
 *   ([storePlayer] over [Player.ControllerOf]) before control changes and the draw reads the
 *   recording — reading the target's controller afterwards would name the caster.
 * - Mode 2: the draw is gated on the control change actually happening
 *   ([SuccessCriterion.ControlChanged]), the Stiltzkin, Moogle Merchant shape.
 */
val RulesOfAcquisition = card("Rules of Acquisition") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Follow the 3rd Rule — Gain control of target nonland permanent an opponent controls. " +
        "That opponent draws three cards.\n" +
        "• Follow the 98th Rule — Target opponent gains control of target nonland permanent you " +
        "control. If they do, you draw three cards."

    spell {
        effect = Effects.Modal(listOf(
            mode("Follow the 3rd Rule — Gain control of target nonland permanent an opponent controls. That opponent draws three cards") {
                val stolen = target(TargetFilter.NonlandPermanentOpponentControls)
                effect = Effects.Pipeline {
                    val thatOpponent = storePlayer(Player.ControllerOf("target nonland permanent"))
                    run(Effects.GainControl(stolen))
                    run(Effects.ForEachPlayer(thatOpponent.asPlayers, Effects.DrawCards(3)))
                }
            },
            mode("Follow the 98th Rule — Target opponent gains control of target nonland permanent you control. If they do, you draw three cards") {
                val opponent = target(Targets.Opponent)
                val given = target(TargetFilter.NonlandPermanent.youControl())
                effect = Effects.IfYouDo(
                    action = Effects.GiveControl(permanent = given, newController = opponent),
                    then = Effects.DrawCards(3),
                    successCriterion = SuccessCriterion.ControlChanged,
                )
            }
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Racrufi"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/307c36ec-6fa1-4d4b-afcd-c3c83b876ee8.jpg?1793013546"
    }
}
