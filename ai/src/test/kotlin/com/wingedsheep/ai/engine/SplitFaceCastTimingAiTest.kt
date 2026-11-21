package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.Speed
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * One half of a split card, cast alone (CR 709.3): HoldPolicy reads what *that half* resolves, and
 * the cast's intent is the half's own — its own type line, so Bustle (a sorcery) is no instant
 * because Hustle, the first face, is one.
 *
 * Hustle — "target creature attacks or blocks this turn if able" — buys something on their turn
 * before they attack (it drags their creature into an attack) and on ours before blocks (into a
 * block). After the turn's fights it buys nothing, so it is floored there rather than cast for free.
 */
class SplitFaceCastTimingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun position(activePlayer: Int, step: Step) = scenario()
        .withPlayers()
        .withTurnNumber(6)
        .withActivePlayer(activePlayer)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(1, "Hustle // Bustle")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .build()
        .advanceToPriority(1, step)

    private fun TestGame.hustleCast(): CastSpell {
        val card = state.getZone(player1Id, Zone.HAND).first { state.getEntity(it)?.get<CardComponent>()?.name == "Hustle // Bustle" }
        return CastSpell(player1Id, card, faceIndex = 0, targets = listOf(ChosenTarget.Permanent(findPermanent("Grizzly Bears")!!)))
    }

    private fun TestGame.hustleVerdict(): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(state, player1Id, "Hustle // Bustle", cast = hustleCast())

    init {
        test("each half's cast reads with its own speed") {
            val game = position(activePlayer = 1, step = Step.PRECOMBAT_MAIN)
            val hustle = game.hustleCast()
            intents.forCast("Hustle // Bustle", hustle)!!.speed shouldBe Speed.INSTANT
            intents.forCast("Hustle // Bustle", hustle.copy(faceIndex = 1, targets = emptyList()))!!.speed shouldBe Speed.SORCERY
        }

        test("Hustle is thrown away after the fights, and live before either player's") {
            withClue("their end step: no attack or block of theirs is left this turn") {
                position(activePlayer = 2, step = Step.END).hustleVerdict() shouldBe TimingVerdict.NoWindow
            }
            withClue("our end step: the same") {
                position(activePlayer = 1, step = Step.END).hustleVerdict() shouldBe TimingVerdict.NoWindow
            }
            withClue("their upkeep: the Bears must attack into our Giant") {
                position(activePlayer = 2, step = Step.UPKEEP).hustleVerdict() shouldNotBe TimingVerdict.NoWindow
            }
            withClue("our precombat main: the Bears must block") {
                position(activePlayer = 1, step = Step.PRECOMBAT_MAIN).hustleVerdict() shouldNotBe TimingVerdict.NoWindow
            }
        }

        test("the audit reads Hustle's timing") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Hustle // Bustle"))
            withClue("$reading") { reading.flags.none { it.key.startsWith("timing") } shouldBe true }
        }
    }
}
