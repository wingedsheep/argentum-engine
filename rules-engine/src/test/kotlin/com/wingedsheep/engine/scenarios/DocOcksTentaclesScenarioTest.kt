package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.DocOcksTentacles
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Doc Ock's Tentacles (SPM #162) — "Whenever a creature you control with mana value 5 or greater
 * enters, you may attach this Equipment to it. Equipped creature gets +4/+4."
 *
 * Pins the auto-attach target: it must attach to the creature that entered (the triggering entity),
 * not to itself. The pre-fix bug used `EffectTarget.Self`, which resolves to the Equipment's own id,
 * so the attach silently never landed on the entering creature.
 */
class DocOcksTentaclesScenarioTest : FunSpec({

    test("a MV>=5 creature entering may attach the Equipment to it (+4/+4)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DocOcksTentacles)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
        val you = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tentacles = driver.putPermanentOnBattlefield(you, "Doc Ock's Tentacles")
        val fon = driver.putCardInHand(you, "Force of Nature") // {3}{G}{G}, 5/5, mana value 5
        driver.giveMana(you, Color.GREEN, 5)
        driver.castSpell(you, fon)

        val forceOfNature = fon
        var guard = 0
        while (guard++ < 40 && (driver.isPaused || driver.state.stack.isNotEmpty())) {
            if (driver.isPaused) {
                when (val dec = driver.pendingDecision) {
                    is YesNoDecision -> driver.submitDecision(dec.playerId, YesNoResponse(dec.id, true))
                    else -> error("unexpected decision resolving Doc Ock's Tentacles: $dec")
                }
            } else {
                driver.bothPass()
            }
        }

        // The Equipment attached to the creature that entered — not to itself.
        driver.state.getEntity(tentacles)?.get<AttachedToComponent>()?.targetId shouldBe forceOfNature

        val projected = StateProjector().project(driver.state)
        projected.getPower(forceOfNature) shouldBe 9   // 5 + 4
        projected.getToughness(forceOfNature) shouldBe 9
    }
})
