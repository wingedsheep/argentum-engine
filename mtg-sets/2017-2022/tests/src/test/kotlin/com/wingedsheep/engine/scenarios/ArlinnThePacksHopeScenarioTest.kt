package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mid.cards.ArlinnThePacksHope
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Arlinn, the Pack's Hope // Arlinn, the Moon's Fury (MID #211).
 *
 * The +1 grants two "until your next turn" effects: flash for creature spells and an
 * enters-with-an-additional-+1/+1-counter replacement. Both must still apply on the opponent's
 * turn and both must be gone once Arlinn's controller's next turn begins.
 */
class ArlinnThePacksHopeScenarioTest : ScenarioTestBase() {

    private val plusOne = ArlinnThePacksHope.activatedAbilities[0].id
    private val minusThree = ArlinnThePacksHope.activatedAbilities[1].id
    private val moonsFuryZero = ArlinnThePacksHope.backFace!!.activatedAbilities[1].id

    private fun TestGame.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.loyalty(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        context("Arlinn, the Pack's Hope") {

            test("+1: flash creatures with an extra +1/+1 counter until your next turn, then both wear off") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arlinn, the Pack's Hope")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .apply { repeat(4) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.copy(dayNight = DayNight.DAY)
                val arlinn = game.findPermanent("Arlinn, the Pack's Hope")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = arlinn, abilityId = plusOne))
                    .error shouldBe null
                game.resolveStack()
                game.loyalty(arlinn) shouldBe 5

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("it is now the opponent's end step") { game.state.activePlayerId shouldBe game.player2Id }
                while (game.state.priorityPlayerId != game.player1Id) game.passPriority()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                val flashed = game.findPermanents("Grizzly Bears").single()
                withClue("the flashed creature entered with the additional +1/+1 counter") {
                    game.plusOneCounters(flashed) shouldBe 1
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("Arlinn's controller's next turn has begun and the replacement grant is gone") {
                    game.state.activePlayerId shouldBe game.player1Id
                    game.state.grantedReplacementEffects.shouldBeEmpty()
                }
                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                val later = game.findPermanents("Grizzly Bears").single { it != flashed }
                game.plusOneCounters(later) shouldBe 0
            }

            test("−3 creates two 2/2 green Wolf tokens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arlinn, the Pack's Hope")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.copy(dayNight = DayNight.DAY)
                val arlinn = game.findPermanent("Arlinn, the Pack's Hope")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = arlinn, abilityId = minusThree))
                    .error shouldBe null
                game.resolveStack()

                game.findPermanents("Wolf Token") shouldHaveSize 2
                game.loyalty(arlinn) shouldBe 1
            }

            test("at night, the Moon's Fury's 0 makes her a 5/5 Werewolf creature that's still a planeswalker") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Arlinn, the Pack's Hope")
                    .apply { repeat(4) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Forest") } }
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.copy(dayNight = DayNight.DAY)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the opponent cast no spells, so it became night and Arlinn flipped") {
                    game.state.activePlayerId shouldBe game.player1Id
                    game.state.dayNight shouldBe DayNight.NIGHT
                    game.findPermanent("Arlinn, the Moon's Fury") shouldNotBe null
                }
                val arlinn = game.findPermanent("Arlinn, the Moon's Fury")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = arlinn, abilityId = moonsFuryZero))
                    .error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.isCreature(arlinn) shouldBe true
                projected.isPlaneswalker(arlinn) shouldBe true
                projected.getPower(arlinn) shouldBe 5
                projected.getToughness(arlinn) shouldBe 5
                projected.hasKeyword(arlinn, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(arlinn, Keyword.INDESTRUCTIBLE) shouldBe true
                projected.hasKeyword(arlinn, Keyword.HASTE) shouldBe true
                game.loyalty(arlinn) shouldBe 4
            }
        }
    }
}
