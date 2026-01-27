package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests verifying that state-based actions correctly use projected toughness
 * (including continuous effects like +3/+3 from Angelic Blessing) rather than base
 * toughness when checking for lethal damage (Rule 704.5g).
 */
class AngelicBlessingScenarioTest : ScenarioTestBase() {

    init {
        context("Buffed creature and lethal damage SBA check") {

            test("creature with Angelic Blessing survives damage less than boosted toughness") {
                // Devoted Hero is 1/2. Angelic Blessing gives +3/+3, making it 4/5.
                // With 4 damage marked, the creature should survive (4 < 5).
                // Bug: SBA checker was using base toughness (2) instead of projected (5),
                // incorrectly killing the creature since 4 >= 2.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardInHand(1, "Angelic Blessing")
                    .withLandsOnBattlefield(1, "Plains", 3)  // Mana for {2}{W}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!

                // Cast and resolve Angelic Blessing on Devoted Hero
                val castResult = game.castSpell(1, "Angelic Blessing", heroId)
                withClue("Casting Angelic Blessing should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Devoted Hero should be on the battlefield after buff") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }

                // Directly apply 4 damage to the creature (simulating combat or spell damage)
                game.state = game.state.updateEntity(heroId) { container ->
                    container.with(DamageComponent(4))
                }

                // Run state-based action check — this is where the bug manifested
                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                // Creature has projected toughness 5 (2 base + 3 from Angelic Blessing)
                // and 4 damage, so it should survive
                withClue("Devoted Hero (4/5 with buff) should survive 4 damage") {
                    game.isOnBattlefield("Devoted Hero") shouldBe true
                }
            }

            test("creature with Angelic Blessing dies to damage equal to boosted toughness") {
                // Devoted Hero is 1/2. Angelic Blessing gives +3/+3, making it 4/5.
                // With 5 damage marked, the creature should die (5 >= 5).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withCardInHand(1, "Angelic Blessing")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!

                // Cast and resolve Angelic Blessing
                game.castSpell(1, "Angelic Blessing", heroId)
                game.resolveStack()

                // Apply 5 damage (equal to boosted toughness)
                game.state = game.state.updateEntity(heroId) { container ->
                    container.with(DamageComponent(5))
                }

                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                withClue("Devoted Hero (4/5 with buff) should die from 5 damage") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
            }

            test("unbuffed creature still dies to damage equal to base toughness") {
                // Sanity check: a plain 1/2 Devoted Hero with 2 damage should die.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Devoted Hero")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!

                game.state = game.state.updateEntity(heroId) { container ->
                    container.with(DamageComponent(2))
                }

                val sbaChecker = StateBasedActionChecker()
                val sbaResult = sbaChecker.checkAndApply(game.state)
                game.state = sbaResult.newState

                withClue("Unbuffed Devoted Hero (1/2) should die from 2 damage") {
                    game.isOnBattlefield("Devoted Hero") shouldBe false
                }
            }
        }
    }
}
