package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ironheart, Clever Champion — {4}{U} Legendary Artifact Creature — Human Hero, 3/4.
 *
 * Improvise (CR 702.126)
 * Flying
 * Noncreature spells you cast have improvise.
 *
 * What this pins:
 *  1. Her own printed improvise: untapped artifacts pay the {4}, leaving only the {U} to mana.
 *  2. The grant reaches a *noncreature* spell — an artifact/instant/sorcery gains improvise while
 *    she is on the battlefield, and its cast surfaces the tap payment.
 *  3. The grant does **not** reach creature spells (CR: the filter is "noncreature spells").
 *  4. The grant is hers, not the table's: it is scoped to spells *you* cast.
 */
class IronheartCleverChampionScenarioTest : ScenarioTestBase() {

    init {
        // A vanilla artifact used as improvise fodder.
        val cog = card("Ironheart Cog") {
            manaCost = "{1}"
            colorIdentity = ""
            typeLine = "Artifact"
            oracleText = ""
        }
        cardRegistry.register(cog)

        // A noncreature spell with no improvise of its own — the grant's subject.
        val noncreature = card("Repulsor Blast") {
            manaCost = "{4}{U}"
            colorIdentity = "U"
            typeLine = "Sorcery"
            oracleText = "You gain 4 life."
            spell { effect = Effects.GainLife(4) }
        }
        cardRegistry.register(noncreature)

        // A creature spell at the same cost — the grant must NOT reach it.
        val creature = card("Stark Intern") {
            manaCost = "{4}{U}"
            colorIdentity = "U"
            typeLine = "Creature — Human"
            power = 2
            toughness = 2
            oracleText = ""
        }
        cardRegistry.register(creature)

        fun castAction(game: TestGame, player: Int, name: String): LegalActionInfo? =
            game.getLegalActions(player).firstOrNull {
                it.actionType == "CastSpell" && it.action is CastSpell && it.description.contains(name)
            }

        test("she is an artifact creature with flying and printed improvise") {
            val def = cardRegistry.getCard("Ironheart, Clever Champion")!!
            def.typeLine.isArtifact shouldBe true
            def.typeLine.isCreature shouldBe true
            def.keywords.contains(Keyword.IMPROVISE) shouldBe true
            def.keywords.contains(Keyword.FLYING) shouldBe true
        }

        test("her own improvise lets four artifacts pay the {4}, leaving only the {U}") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Ironheart, Clever Champion")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cogs = game.findAllPermanents("Ironheart Cog")
            val action = castAction(game, 1, "Ironheart, Clever Champion")
            withClue("one Island plus four Cogs should make {4}{U} affordable") {
                action shouldNotBe null
                action!!.isAffordable shouldBe true
                action.hasTapForGeneric shouldBe true
                action.tapForGenericLabel shouldBe "improvise"
            }

            val cast = (action!!.action as CastSpell).copy(
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = cogs.toSet())
            )
            val result = game.execute(cast)
            withClue("the cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()
            game.isOnBattlefield("Ironheart, Clever Champion") shouldBe true
            cogs.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
        }

        test("noncreature spells she grants improvise to can tap artifacts too") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Ironheart, Clever Champion")
                .withCardInHand(1, "Repulsor Blast")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Ironheart herself is an untapped artifact, so she is a fourth improvise source.
            val ironheart = game.findPermanent("Ironheart, Clever Champion")!!
            val cogs = game.findAllPermanents("Ironheart Cog")
            val before = game.getLifeTotal(1)

            val action = castAction(game, 1, "Repulsor Blast")
            withClue("the granted improvise should make the sorcery castable and offer the taps") {
                action shouldNotBe null
                action!!.isAffordable shouldBe true
                action.hasTapForGeneric shouldBe true
                action.tapForGenericLabel shouldBe "improvise"
            }

            val taps = (cogs + ironheart).toSet()
            val cast = (action!!.action as CastSpell).copy(
                alternativePayment = AlternativePaymentChoice(tapForGenericPermanents = taps)
            )
            val result = game.execute(cast)
            withClue("three Cogs plus Ironheart should cover the {4}: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe before + 4
        }

        test("the grant does not reach creature spells") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Ironheart, Clever Champion")
                .withCardInHand(1, "Stark Intern")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withCardOnBattlefield(1, "Ironheart Cog")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val action = castAction(game, 1, "Stark Intern")
            withClue("a creature spell gets no improvise, so one Island can't pay {4}{U}") {
                (action == null || !action.isAffordable) shouldBe true
                action?.hasTapForGeneric shouldNotBe true
            }
        }

        test("the grant is scoped to spells you cast, not the opponent's") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Ironheart, Clever Champion")
                .withCardInHand(2, "Repulsor Blast")
                .withLandsOnBattlefield(2, "Island", 1)
                .withCardOnBattlefield(2, "Ironheart Cog")
                .withCardOnBattlefield(2, "Ironheart Cog")
                .withCardOnBattlefield(2, "Ironheart Cog")
                .withCardOnBattlefield(2, "Ironheart Cog")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val action = castAction(game, 2, "Repulsor Blast")
            withClue("the opponent's noncreature spell gets no improvise from my Ironheart") {
                (action == null || !action.isAffordable) shouldBe true
                action?.hasTapForGeneric shouldNotBe true
            }
        }
    }
}
