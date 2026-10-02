package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hydroelectric Specimen // Hydroelectric Laboratory (MH3).
 *
 * Front: "Flash. When this creature enters, you may change the target of target instant or sorcery
 * spell with a single target to this creature." Back: "As this land enters, you may pay 3 life. If
 * you don't, it enters tapped. {T}: Add {U}."
 *
 * Proves `withSingleTarget()` as a targeting restriction and `Effects.ChangeTarget(to = Self)` as a
 * no-choice redirect that only lands on a legal target (CR 115.7a).
 */
class HydroelectricSpecimenScenarioTest : ScenarioTestBase() {

    /** Opponent's turn, opponent holding [spell] and the mana for it; we hold Specimen + four Islands. */
    private fun game(spell: String, opponentLands: Pair<String, Int>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Hydroelectric Specimen")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(2, spell)
        .withLandsOnBattlefield(2, opponentLands.first, opponentLands.second)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** We flash in Specimen over the opponent's spell and let it resolve; its trigger then wants a target. */
    private fun TestGame.flashInSpecimen() {
        passPriority() // opponent passes with their spell on the stack
        castSpell(1, "Hydroelectric Specimen").error shouldBe null
        passPriority()
        passPriority() // Specimen resolves; its enters trigger asks for a target
    }

    /**
     * Use the optional trigger on [spell] — the "you may" is asked as the trigger is put on the
     * stack, then its target is chosen (auto-picked when [spell] is the only legal one) — and
     * resolve the trigger, leaving [spell] alone on the stack.
     */
    private fun TestGame.useTriggerOn(spell: com.wingedsheep.sdk.model.EntityId) {
        getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        answerYesNo(true).error shouldBe null
        if (getPendingDecision() is ChooseTargetsDecision) selectTargets(listOf(spell)).error shouldBe null
        var guard = 0
        while (state.stack.size > 1 && getPendingDecision() == null && guard++ < 10) passPriority()
        state.stack shouldBe listOf(spell)
    }

    private fun TestGame.targetsOf(spellId: com.wingedsheep.sdk.model.EntityId) =
        state.getEntity(spellId)!!.get<TargetsComponent>()!!.targets

    init {
        context("Hydroelectric Specimen — redirect a single-target spell to itself") {

            test("an opponent's Lightning Bolt at our Bears is redirected to Specimen, which survives it") {
                val game = game("Lightning Bolt", "Mountain" to 1)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
                val bolt = game.state.stack.last()

                game.flashInSpecimen()
                val specimen = game.findPermanent("Hydroelectric Specimen")!!
                game.useTriggerOn(bolt)

                withClue("the Bolt's only target is now Specimen") {
                    game.targetsOf(bolt) shouldBe listOf(ChosenTarget.Permanent(specimen))
                }
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hydroelectric Specimen") shouldBe true
                game.state.getEntity(specimen)!!.get<DamageComponent>()?.amount shouldBe 3
            }

            test("a spell whose only target can't be Specimen keeps its target (Ancestral Recall)") {
                val game = game("Ancestral Recall", "Island" to 1)
                game.castSpellTargetingPlayer(2, "Ancestral Recall", 2).error shouldBe null
                val recall = game.state.stack.last()

                game.flashInSpecimen()
                game.useTriggerOn(recall)

                withClue("Specimen isn't a legal target for 'target player' — CR 115.7a leaves it unchanged") {
                    game.targetsOf(recall) shouldBe listOf(ChosenTarget.Player(game.player2Id))
                }
            }

            test("a spell with two targets can't be targeted by the trigger") {
                val game = game("Twin Bolt", "Mountain" to 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                val twinBolt = game.state.getHand(game.player2Id).single()
                game.execute(
                    com.wingedsheep.engine.core.CastSpell(
                        playerId = game.player2Id,
                        cardId = twinBolt,
                        targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Player(game.player1Id)),
                        damageDistribution = mapOf(bears to 1, game.player1Id to 1),
                    )
                ).error shouldBe null
                game.targetsOf(game.state.stack.last()).size shouldBe 2

                val twinBoltOnStack = game.state.stack.last()

                game.flashInSpecimen()
                withClue("no legal target, so the trigger is removed without asking (CR 603.3d)") {
                    game.getPendingDecision() shouldBe null
                    game.state.stack shouldBe listOf(twinBoltOnStack)
                }
            }

            test("the same spell with a single target is a legal choice") {
                val game = game("Twin Bolt", "Mountain" to 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Twin Bolt", bears).error shouldBe null
                val twinBolt = game.state.stack.last()
                game.flashInSpecimen()
                game.useTriggerOn(twinBolt)
                game.targetsOf(twinBolt) shouldBe
                    listOf(ChosenTarget.Permanent(game.findPermanent("Hydroelectric Specimen")!!))
            }

            test("declining leaves the spell's target alone") {
                val game = game("Lightning Bolt", "Mountain" to 1)
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
                val bolt = game.state.stack.last()

                game.flashInSpecimen()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null

                game.targetsOf(bolt) shouldBe listOf(ChosenTarget.Permanent(bears))
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }

        context("Hydroelectric Laboratory — the land back") {

            test("played as a land, paying 3 life has it enter untapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hydroelectric Specimen")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Hydroelectric Laboratory")
                land shouldNotBe null
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land!!)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hydroelectric Specimen")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Hydroelectric Laboratory")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
