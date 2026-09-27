package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A transforming double-faced card whose back face is an instant or sorcery
 * ([CardDefinition.doubleFacedWithSpellBack]) — the Siege shape of Invasion of Kylem // Valor's
 * Reach Tag Team.
 *
 * The rules the shape depends on, each pinned below:
 *  - CR 712.11a / 712.8c — the defeat trigger casts it transformed: the sorcery back face is the
 *    spell, with its own targets, and resolves like any sorcery.
 *  - CR 712.8a — once it resolves it is in its owner's graveyard with only its front face's
 *    characteristics again.
 *  - CR 712.10 — a transform instruction on the front-face permanent does nothing.
 *  - CR 712.14a with CR 400.4a — told to return to the battlefield transformed, the card stays in
 *    the zone it is in.
 */
class SiegeSpellBackFaceScenarioTest : ScenarioTestBase() {

    private val siegeBack = card("Test Siege Volley") {
        manaCost = ""
        colorIdentity = "R"
        colorIndicator = "R"
        typeLine = "Sorcery"
        oracleText = "Target player loses 3 life."

        spell {
            val p = target(Targets.Player)
            effect = Effects.LoseLife(3, p)
        }
    }

    private val siegeFront = card("Test Spell Siege") {
        manaCost = "{2}{R}"
        colorIdentity = "R"
        typeLine = "Battle — Siege"
        startingDefense = 3
        oracleText = "(As a Siege enters, choose an opponent to protect it.)"
    }

    private val spellSiege: CardDefinition = CardDefinition.doubleFacedWithSpellBack(
        frontFace = siegeFront,
        backFace = siegeBack,
    )

    private val transformSpell = card("Test Turnover") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        oracleText = "Transform target permanent."

        spell {
            target = TargetObject(filter = TargetFilter.Permanent)
            effect = Effects.Transform(EffectTarget.ContextTarget(0))
        }
    }

    private val flickerTransformedSpell = card("Test Turnabout") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        oracleText = "Exile target permanent, then return it to the battlefield transformed " +
            "under its owner's control."

        spell {
            target = TargetObject(filter = TargetFilter.Permanent)
            effect = Effects.ExileAndReturnTransformed(EffectTarget.ContextTarget(0))
        }
    }

    private fun siegeInPlay(vararg hand: String): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Spell Siege")
            .withLandsOnBattlefield(1, "Mountain", hand.size)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        hand.forEach { builder.withCardInHand(1, it) }
        val game = builder.build()
        game.checkStateBasedActions()
        return game
    }

    init {
        cardRegistry.register(spellSiege)
        cardRegistry.register(transformSpell)
        cardRegistry.register(flickerTransformedSpell)

        context("the factory") {
            test("rejects a permanent back face — that shape is doubleFacedPermanent") {
                shouldThrow<IllegalArgumentException> {
                    CardDefinition.doubleFacedWithSpellBack(siegeFront, siegeFront)
                }
            }
        }

        context("CR 712.11a — the defeat trigger casts the sorcery back face") {

            test("the back face is cast with its own target, resolves, and the card is binned front face up") {
                val game = siegeInPlay("Lightning Bolt")

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Test Spell Siege")).error shouldBe null
                game.resolveStack()
                withClue("defeated: exiled, then offered the transformed cast") {
                    game.isInExile(1, "Test Spell Siege") shouldBe true
                }
                game.answerYesNo(true).error shouldBe null

                withClue("the sorcery face's target is chosen as it is cast") {
                    game.selectTargets(listOf(game.player2Id)).error shouldBe null
                }
                game.resolveStack()

                withClue("the back face's spell effect resolved") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("an instant or sorcery never becomes a permanent") {
                    game.isOnBattlefield("Test Siege Volley") shouldBe false
                    game.isOnBattlefield("Test Spell Siege") shouldBe false
                }
                withClue("CR 712.8a — in the graveyard it has only its front face's characteristics") {
                    game.isInGraveyard(1, "Test Spell Siege") shouldBe true
                    game.isInGraveyard(1, "Test Siege Volley") shouldBe false
                    game.isInExile(1, "Test Spell Siege") shouldBe false
                }
            }

            test("declining the cast leaves the card in exile") {
                val game = siegeInPlay("Lightning Bolt")

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Test Spell Siege")).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Test Spell Siege") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }
        }

        context("CR 712.10 — transforming into an instant or sorcery face does nothing") {

            test("a transform instruction leaves the Siege on its front face") {
                val game = siegeInPlay("Test Turnover")
                val siegeId = game.findPermanent("Test Spell Siege")!!

                game.castSpell(1, "Test Turnover", siegeId).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Test Spell Siege") shouldBe true
                game.state.getEntity(siegeId)?.get<CardComponent>()?.name shouldBe "Test Spell Siege"
            }
        }

        context("CR 712.14a with CR 400.4a — a sorcery face can't enter the battlefield") {

            test("exiled and told to return transformed, the card stays in exile") {
                val game = siegeInPlay("Test Turnabout")

                game.castSpell(1, "Test Turnabout", game.findPermanent("Test Spell Siege")).error shouldBe null
                game.resolveStack()

                withClue("the exile happens; the return does not") {
                    game.isOnBattlefield("Test Spell Siege") shouldBe false
                    game.isOnBattlefield("Test Siege Volley") shouldBe false
                    game.isInExile(1, "Test Spell Siege") shouldBe true
                }
            }
        }
    }
}
