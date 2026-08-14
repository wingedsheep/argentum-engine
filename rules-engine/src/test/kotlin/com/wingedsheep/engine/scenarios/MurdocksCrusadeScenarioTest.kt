package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Murdock's Crusade (MSH #24) — {1}{W} Sorcery.
 *
 *   Teamwork 4
 *   Choose one. If this spell was cast using teamwork, choose both instead.
 *   • Street Justice — Exile target creature with toughness 4 or greater.
 *   • Legal Justice — Exile target enchantment with mana value 4 or greater.
 *
 * Wall of Swords (3/5) and Castle ({3}{W}, mana value 4) are the two legal victims; Grizzly Bears
 * (2/2) is on the board as the control that the toughness restriction actually restricts.
 */
class MurdocksCrusadeScenarioTest : ScenarioTestBase() {

    init {
        context("Murdock's Crusade") {

            test("cast without teamwork resolves only the one chosen mode") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Murdock's Crusade")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withCardOnBattlefield(2, "Castle")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Murdock's Crusade").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(wall)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(wall))),
                    ),
                ).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Wall of Swords") shouldBe true
                withClue("Legal Justice was not chosen, so the enchantment stays") {
                    game.isOnBattlefield("Castle") shouldBe true
                }
                withClue("no teamwork was declared, so nothing tapped") {
                    game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe false
                }
            }

            test("cast using teamwork resolves both modes") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Murdock's Crusade")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withCardOnBattlefield(2, "Castle")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()
                val castle = game.findPermanent("Castle").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Murdock's Crusade").first()

                // Teamwork 4 — the 6/4 Craw Wurm clears the threshold on its own.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(wall),
                            ChosenTarget.Permanent(castle),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(wall)),
                            listOf(ChosenTarget.Permanent(castle)),
                        ),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(wurm),
                        ),
                    ),
                ).error shouldBe null
                game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                game.isInExile(2, "Wall of Swords") shouldBe true
                game.isInExile(2, "Castle") shouldBe true
            }

            test("choosing one mode with teamwork declared is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Murdock's Crusade")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withCardOnBattlefield(2, "Castle")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Murdock's Crusade").first()

                // "Choose both instead" is not an allowance — a cast that declared teamwork owes
                // both modes, so a one-mode submission is illegal (CR 601.2, 700.2a).
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(wall)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(wall))),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(wurm),
                        ),
                    ),
                ).error.shouldNotBeNull()

                withClue("the rejected cast is rewound whole") {
                    game.isInHand(1, "Murdock's Crusade") shouldBe true
                    game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe false
                    game.isOnBattlefield("Wall of Swords") shouldBe true
                }
            }

            test("choosing both modes without teamwork is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Murdock's Crusade")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withCardOnBattlefield(2, "Castle")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wall = game.findPermanent("Wall of Swords").shouldNotBeNull()
                val castle = game.findPermanent("Castle").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Murdock's Crusade").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(wall),
                            ChosenTarget.Permanent(castle),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(wall)),
                            listOf(ChosenTarget.Permanent(castle)),
                        ),
                    ),
                ).error.shouldNotBeNull()
                game.isInHand(1, "Murdock's Crusade") shouldBe true
            }

            // The three tests above drive `execute(CastSpell(...))` and so prove only what the
            // *handler* accepts. These two go against `getLegalActions`, which is what the client
            // actually sees, and pin the two halves the handler can't: the advertised mode count
            // on the plain cast, and whether the teamwork variant is offered at all.

            test("the enumerator advertises one mode plainly, two with teamwork, and filters each mode's targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Murdock's Crusade")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    .withCardOnBattlefield(2, "Castle")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val castle = game.findPermanent("Castle").shouldNotBeNull()

                val modalCasts = game.getLegalActions(1).filter { it.actionType == "CastSpellModal" }

                val plainCast = modalCasts
                    .firstOrNull { it.additionalCostInfo?.costType != "TapForTotalPower" }
                    .shouldNotBeNull()
                val plainModal = plainCast.modalEnumeration.shouldNotBeNull()
                withClue("no teamwork declared, so the dynamic count is 1 — advertising 2 would let " +
                    "the client submit a two-mode cast the handler then rejects") {
                    plainModal.chooseCount shouldBe 1
                    plainModal.minChooseCount shouldBe 1
                }

                val teamworkCast = modalCasts
                    .firstOrNull { it.additionalCostInfo?.costType == "TapForTotalPower" }
                    .shouldNotBeNull()
                teamworkCast.description shouldBe "Cast Murdock's Crusade (Teamwork 4)"
                val teamworkModal = teamworkCast.modalEnumeration.shouldNotBeNull()
                teamworkModal.chooseCount shouldBe 2
                teamworkModal.modes.map { it.available } shouldBe listOf(true, true)

                // Per-mode target enumeration: each mode carries its own requirement, and each
                // filter excludes the permanent it is supposed to exclude.
                val streetJustice = teamworkModal.modes[0].targetRequirements.single()
                withClue("toughness 4 or greater: the 6/4 Craw Wurm qualifies, the 2/2 Bears do not") {
                    streetJustice.validTargets shouldContain wurm
                    streetJustice.validTargets shouldNotContain bears
                }
                val legalJustice = teamworkModal.modes[1].targetRequirements.single()
                withClue("mana value 4 or greater: {3}{W} Castle qualifies") {
                    legalJustice.validTargets shouldBe listOf(castle)
                }
            }

            test("the teamwork variant is not offered when only one mode has a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Murdock's Crusade")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(2, "Wall of Swords")
                    // {G}{G}, mana value 2 — an enchantment, but under the threshold.
                    .withCardOnBattlefield(2, "Lifeforce")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val modalCasts = game.getLegalActions(1).filter { it.actionType == "CastSpellModal" }

                withClue("teamwork means choosing both modes, and Legal Justice has no legal " +
                    "target (CR 700.2a), so the teamwork cast could never be completed") {
                    modalCasts.none { it.additionalCostInfo?.costType == "TapForTotalPower" } shouldBe true
                }

                // The plain one-mode cast is still on offer — Street Justice has a victim.
                val plainCast = modalCasts.single()
                val plainModal = plainCast.modalEnumeration.shouldNotBeNull()
                plainModal.chooseCount shouldBe 1
                plainModal.modes.map { it.available } shouldBe listOf(true, false)
                plainModal.modes[1].targetRequirements.single().validTargets.shouldBeEmpty()
            }
        }
    }
}
