package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.SyrKonradTheGrim
import com.wingedsheep.mtg.sets.definitions.lea.cards.RaiseDead
import com.wingedsheep.mtg.sets.definitions.lea.cards.WrathOfGod
import com.wingedsheep.mtg.sets.definitions.one.cards.VatEmergence
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Syr Konrad, the Grim (ELD #107).
 *
 * "Whenever another creature dies, or a creature card is put into a graveyard from anywhere other
 *  than the battlefield, or a creature card leaves your graveyard, Syr Konrad deals 1 damage to
 *  each opponent. {1}{B}: Each player mills a card."
 *
 * The printed trigger is split across two abilities (OTHER-bound dies, ANY-bound zone changes) and
 * the "anywhere other than the battlefield" clause is a union of source zones, so each clause gets
 * exercised here, plus the ruling that creatures dying alongside Konrad still trigger it.
 */
class SyrKonradTheGrimScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(SyrKonradTheGrim)
        cardRegistry.register(RaiseDead)
        cardRegistry.register(WrathOfGod)
        cardRegistry.register(VatEmergence)

        context("Syr Konrad, the Grim") {

            test("milling a creature card from each library triggers once per creature card") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val konrad = game.findPermanent("Syr Konrad, the Grim")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = konrad,
                        abilityId = SyrKonradTheGrim.activatedAbilities.single().id,
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("two creature cards hit graveyards from libraries — two pings to the opponent") {
                    game.getLifeTotal(2) shouldBe 18
                }
                game.getLifeTotal(1) shouldBe 20
            }

            test("milling a noncreature card does nothing") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(2, "Swamp")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val konrad = game.findPermanent("Syr Konrad, the Grim")!!
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = konrad,
                        abilityId = SyrKonradTheGrim.activatedAbilities.single().id,
                    )
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("another creature dying triggers it; the dying creature is counted once") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("a death is from the battlefield, so only the dies clause fires") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("creatures dying at the same time as Konrad still trigger it, but Konrad itself doesn't") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Wrath of God")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wrath of God").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isOnBattlefield("Syr Konrad, the Grim") shouldBe false
                withClue("two other creatures died — two pings; Konrad's own death isn't 'another'") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("a creature card leaving your graveyard triggers it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Raise Dead")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Raise Dead", 1, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 19
            }

            test("a creature card leaving an opponent's graveyard doesn't trigger it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInHand(2, "Raise Dead")
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(2, "Raise Dead", 2, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("an opponent reanimating your creature card still triggers it — it left your graveyard") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(2, "Vat Emergence")
                    .withLandsOnBattlefield(2, "Swamp", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(2, "Vat Emergence", 1, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 19
            }

            test("reanimating an opponent's creature card under your control doesn't trigger it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInHand(1, "Vat Emergence")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Vat Emergence", 2, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("a countered creature spell going to the graveyard from the stack triggers it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Syr Konrad, the Grim")
                    .withCardInHand(1, "Counterspell")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsSpell = game.findCardsInHand(2, "Grizzly Bears").single()
                game.castSpell(2, "Grizzly Bears").error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.passPriority()

                val counterspell = game.findCardsInHand(1, "Counterspell").single()
                game.execute(
                    CastSpell(game.player1Id, counterspell, listOf(ChosenTarget.Spell(bearsSpell)))
                ).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 19
            }
        }
    }
}
