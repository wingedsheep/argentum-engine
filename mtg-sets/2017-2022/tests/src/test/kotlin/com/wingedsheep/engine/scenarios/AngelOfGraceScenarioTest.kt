package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Angel of Grace (RNA #1) — ETB: until end of turn, damage that would reduce your life total to
 * less than 1 reduces it to 1 instead. {4}{W}{W}, exile it from your graveyard: your life total
 * becomes 10.
 */
class AngelOfGraceScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Angel of Grace")!!.activatedAbilities.single().id

    init {
        test("the ETB floor keeps damage at 1 for the turn, even after the Angel is gone") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Angel of Grace")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Plains", 5)
                // Five Mountains: auto-pay may spend up to three on the Angel's generic cost.
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withLifeTotal(1, 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Mountain") } }
                .build()

            game.castSpell(1, "Angel of Grace").error shouldBe null
            game.resolveStack()

            val angel = game.findPermanent("Angel of Grace")!!
            game.state = game.state.moveToZone(
                angel, ZoneKey(game.player1Id, Zone.BATTLEFIELD), ZoneKey(game.player1Id, Zone.GRAVEYARD)
            )

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            withClue("3 damage at 3 life leaves Alice at 1") { game.getLifeTotal(1) shouldBe 1 }

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            withClue("at 1 life, further damage doesn't reduce it") { game.getLifeTotal(1) shouldBe 1 }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the floor ends with the turn") { game.state.grantedReplacementEffects.shouldBeEmpty() }
        }

        test("without the ETB, damage reduces life normally") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLifeTotal(1, 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 1
        }

        test("exiling it from the graveyard sets your life total to 10") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInGraveyard(1, "Angel of Grace")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withLifeTotal(1, 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val angel = game.findCardsInGraveyard(1, "Angel of Grace").single()
            game.execute(ActivateAbility(game.player1Id, angel, abilityId)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 10
            game.isInExile(1, "Angel of Grace") shouldBe true
        }
    }
}
