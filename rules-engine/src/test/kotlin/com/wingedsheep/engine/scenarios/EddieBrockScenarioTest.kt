package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.spm.cards.EddieBrock
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Eddie Brock // Venom, Lethal Protector (SPM #55) —
 * a transforming double-faced Legendary Creature.
 *
 * Front — Eddie Brock · {2}{B} · 3/3:
 *   ETB: return target creature card with mana value 1 or less from your graveyard to the battlefield.
 *   {3}{B}{R}{G}: Transform Eddie Brock. Activate only as a sorcery.
 *
 * Back — Venom, Lethal Protector · 5/5, Menace, trample, haste:
 *   Whenever Venom attacks, you may sacrifice another creature. If you do, draw X cards, then you
 *   may put a permanent card with mana value X or less from your hand onto the battlefield, where
 *   X is the sacrificed creature's mana value.
 */
class EddieBrockScenarioTest : ScenarioTestBase() {

    init {
        // The front face's only non-mana activated ability is the Transform ability.
        val transformAbilityId = EddieBrock.activatedAbilities.first { !it.isManaAbility }.id

        context("Eddie Brock — front face") {

            test("ETB reanimates a mana-value-1 creature card from your graveyard") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Eddie Brock")
                    .withCardInGraveyard(1, "Savannah Lions") // {W}, mana value 1
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Eddie Brock")
                withClue("Eddie Brock should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val lion = game.findCardsInGraveyard(1, "Savannah Lions").first()
                game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the ETB reanimation; got ${game.getPendingDecision()}")
                game.selectTargets(listOf(lion))
                game.resolveStack()

                withClue("Savannah Lions was returned from the graveyard to the battlefield") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
            }

            test("{3}{B}{R}{G} sorcery-speed ability transforms Eddie into Venom, Lethal Protector") {
                // Cast Eddie from hand so the engine attaches the DoubleFacedComponent that the
                // Transform ability needs (the scenario builder only wires it for back faces).
                // Empty graveyard → the ETB has no legal target and is removed from the stack.
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Eddie Brock")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Eddie Brock")
                withClue("Eddie Brock should cast: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val eddie = game.findPermanent("Eddie Brock")!!
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = eddie, abilityId = transformAbilityId))
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Eddie flipped to his back face") {
                    game.state.getEntity(eddie)!!.get<CardComponent>()!!.name shouldBe "Venom, Lethal Protector"
                }
            }
        }

        context("Venom, Lethal Protector — back face") {

            test("attacking, sacrificing a creature draws X and puts an X-or-less permanent from hand onto the battlefield") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Venom, Lethal Protector", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")  // {1}{G}, mana value 2 → X = 2
                    .withCardInHand(1, "Savannah Lions")         // {W}, mana value 1 (≤ X) → puttable
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")              // two cards so drawing X=2 succeeds
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1) // 1 (Savannah Lions)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Venom, Lethal Protector" to 2)).error shouldBe null
                game.resolveStack()

                // "you may sacrifice another creature" — yes.
                withClue("attack offers the optional sacrifice") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true)

                // The sacrifice is a resolution-time choice; Grizzly Bears is the only other creature.
                val grizzly = game.findPermanent("Grizzly Bears")!!
                withClue("choosing the creature to sacrifice") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(grizzly))
                if (game.getPendingDecision() == null) game.resolveStack()

                // "draw X cards, then you may put a permanent card with mana value X or less ...".
                withClue("a put-permanent-from-hand prompt is offered") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe true
                }
                val lion = game.findCardsInHand(1, "Savannah Lions").first()
                game.selectCards(listOf(lion))
                game.resolveStack()

                withClue("Grizzly Bears was sacrificed") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("drew X=2 then put one card from hand → net hand +1") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                withClue("Savannah Lions entered the battlefield from hand") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
            }
        }
    }
}
