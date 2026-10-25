package com.wingedsheep.engine

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceKeywordAction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Learn (CR 701.48) as a *replaceable* keyword action: a [ReplaceKeywordAction] over
 * [EventPattern.LearnEvent] performs its effect instead of the learn (CR 614.1a). Card-level
 * coverage of the printed card lives in `RetrieverPhoenixScenarioTest`; this file pins the
 * mechanism with synthetic cards:
 *
 *  - a mandatory replacement applies without a prompt, and the learn doesn't happen;
 *  - `activeZones` decides where the source functions (CR 113.6) — a battlefield-only source does
 *    nothing from the graveyard, a graveyard-only one nothing from the battlefield;
 *  - the replacement effect runs rooted at its own source (`EffectTarget.Self` is the source);
 *  - `LearnEvent.player` is measured from the source's controller ("an opponent would learn").
 */
class LearnReplacementTest : FunSpec({

    /** "{T}: Learn." */
    val Learner = card("Learn Source") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}: Learn."
        activatedAbility {
            cost = Costs.Tap
            effect = Patterns.Mechanic.learn()
        }
    }
    val learnAbilityId = Learner.script.activatedAbilities.single().id

    /** "{T}: Learn, then you gain 1 life." — the learn sits mid-pipeline. */
    val LearnThenGain = card("Learn Then Gain") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}: Learn, then you gain 1 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Patterns.Mechanic.learn() then Effects.GainLife(1)
        }
    }
    val learnThenGainAbilityId = LearnThenGain.script.activatedAbilities.single().id

    /** "If you would learn, instead you gain 5 life." — mandatory, from the battlefield. */
    val LifeInstead = card("Life Instead") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "If you would learn, instead you gain 5 life."
        replacementEffect(ReplaceKeywordAction(Effects.GainLife(5), EventPattern.LearnEvent()))
    }

    /** "If an opponent would learn, instead you gain 5 life." */
    val OpponentLifeInstead = card("Opponent Life Instead") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "If an opponent would learn, instead you gain 5 life."
        replacementEffect(
            ReplaceKeywordAction(Effects.GainLife(5), EventPattern.LearnEvent(Player.EachOpponent))
        )
    }

    /** "As long as this card is in your graveyard, if you would learn, you may instead exile it." */
    val GraveyardExiler = card("Graveyard Exiler") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "As long as this card is in your graveyard, if you would learn, you may instead exile this card."
        replacementEffect(
            ReplaceKeywordAction(
                Effects.Exile(EffectTarget.Self),
                EventPattern.LearnEvent(),
                optional = true,
                activeZones = setOf(Zone.GRAVEYARD)
            )
        )
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Learner, LearnThenGain, LifeInstead, OpponentLifeInstead, GraveyardExiler))
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun learn(driver: GameTestDriver, player: EntityId) {
        val source = driver.putPermanentOnBattlefield(player, "Learn Source")
        driver.submit(ActivateAbility(playerId = player, sourceId = source, abilityId = learnAbilityId))
            .outcome shouldBe Outcome.Done
        driver.bothPass()
    }

    test("a mandatory replacement applies without a prompt, and the learn doesn't happen") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Life Instead")

        learn(driver, player)

        withClue("no learn discard offer and no yes/no — the replacement just applied") {
            driver.pendingDecision shouldBe null
        }
        driver.getLifeTotal(player) shouldBe 25
    }

    test("a battlefield replacement does nothing from the graveyard") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val life = driver.putPermanentOnBattlefield(player, "Life Instead")
        driver.moveToGraveyard(life)

        learn(driver, player)

        withClue("the learn runs as printed — its discard is offered") {
            driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        }
        driver.getLifeTotal(player) shouldBe 20
    }

    test("a graveyard replacement does nothing from the battlefield") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Graveyard Exiler")

        learn(driver, player)

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("a graveyard replacement runs rooted at its own source") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val exiler = driver.putPermanentOnBattlefield(player, "Graveyard Exiler")
        driver.moveToGraveyard(exiler)
        val graveyardCard = driver.state.getGraveyard(player).single()

        learn(driver, player)

        val prompt = driver.pendingDecision
        prompt.shouldBeInstanceOf<YesNoDecision>()
        withClue("the source's controller — the graveyard's owner — answers") {
            prompt.playerId shouldBe player
        }
        driver.submitDecision(player, YesNoResponse(prompt.id, true))

        withClue("EffectTarget.Self is the graveyard card, not the ability that said learn") {
            driver.state.getExile(player).contains(graveyardCard) shouldBe true
        }
        driver.pendingDecision shouldBe null
    }

    test("the effects after a replaced learn still run, whichever way the prompt is answered") {
        for (answer in listOf(true, false)) {
            val driver = newDriver()
            val player = driver.activePlayer!!
            val exiler = driver.putPermanentOnBattlefield(player, "Graveyard Exiler")
            driver.moveToGraveyard(exiler)
            val source = driver.putPermanentOnBattlefield(player, "Learn Then Gain")
            driver.submit(ActivateAbility(playerId = player, sourceId = source, abilityId = learnThenGainAbilityId))
                .outcome shouldBe Outcome.Done
            driver.bothPass()

            val prompt = driver.pendingDecision
            prompt.shouldBeInstanceOf<YesNoDecision>()
            driver.submitDecision(player, YesNoResponse(prompt.id, answer))
            if (!answer) {
                // Declined: the learn's own discard offer — pass on it.
                val discard = driver.pendingDecision
                discard.shouldBeInstanceOf<SelectCardsDecision>()
                driver.submitDecision(player, CardsSelectedResponse(discard.id, emptyList()))
            }

            withClue("answered ${if (answer) "yes" else "no"}: the trailing life gain resolved") {
                driver.getLifeTotal(player) shouldBe 21
            }
        }
    }

    test("'an opponent would learn' is measured from the replacement's controller") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != player }
        driver.putPermanentOnBattlefield(player, "Opponent Life Instead")

        learn(driver, player)

        withClue("its own controller learning isn't replaced") {
            driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            driver.getLifeTotal(player) shouldBe 20
            driver.getLifeTotal(opponent) shouldBe 20
        }
    }
})
