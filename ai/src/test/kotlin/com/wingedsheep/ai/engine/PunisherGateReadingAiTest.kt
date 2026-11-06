package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe

/**
 * A gate another player decides ([com.wingedsheep.sdk.scripting.effects.GatedEffect] with a
 * `decisionMaker`, the punisher shape). The card does one branch, the decider's pick, so it is
 * *for* whatever either branch does — and *worth* its weaker branch, the one the decider takes.
 *
 * Clash of Elements: "its owner may put it on top of their library … if they didn't, they put it
 * on the bottom". Both branches tuck the target, so the card is removal and the slot is harmful.
 * Unread, nothing stopped the AI from casting it at its own creature when the opponent's only
 * nonland permanent was hexproof.
 *
 * Risk Factor and Combustible Gearhulk: four damage to the targeted opponent or three cards for
 * us. Read as neither, the Gearhulk's ETB and the whole of Risk Factor meant nothing to the AI.
 * A tax ("unless its controller pays {1}", Crystal Shard) is not a punisher: the decider can
 * always buy their way out, so it stays unread.
 */
class PunisherGateReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an opponent's choice between two tucks reads as removal") {
            withClue("Clash of Elements: top or bottom of its owner's library, the owner's pick") {
                TargetPolarityAnalyzer.forSpell(card("Clash of Elements")) shouldBe listOf(TargetPolarity.HARMFUL)
                CardIntentAnalyzer.analyze(card("Clash of Elements")).tags shouldContain IntentTag.REMOVAL
            }
        }

        test("a punisher between two payoffs reads as either payoff, read against its target slot") {
            withClue("Risk Factor: four damage to the target opponent, or three cards for us") {
                val tags = CardIntentAnalyzer.analyze(card("Risk Factor")).tags
                tags shouldContain IntentTag.DRAW
                tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
                // Damage to a slot only a player fills is a clock, not an answer to a creature.
                tags shouldNotContain IntentTag.REMOVAL
            }
            withClue("Combustible Gearhulk's ETB: three cards, or milled damage to that player") {
                val trigger = card("Combustible Gearhulk").script.triggeredAbilities.single()
                val reqs = listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements
                val tags = CardIntentAnalyzer.analyzeEffect(trigger.effect, reqs).tags
                tags shouldContain IntentTag.DRAW
                tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
            }
            withClue("Crystal Shard: a bounce its controller can pay {1} to stop is a tax, not a punisher") {
                CardIntentAnalyzer.analyze(card("Crystal Shard")).tags shouldNotContain IntentTag.REMOVAL
            }
        }

        test("a branch that would win us the game is one no decider picks") {
            val probe = card("Punisher Win Probe") {
                manaCost = "{2}{U}"
                typeLine = "Sorcery"
                spell {
                    val opponent = target(Targets.Opponent)
                    effect = Effects.May(
                        effect = Effects.WinGame(EffectTarget.Controller),
                        otherwise = Effects.DrawCards(1),
                        decisionMaker = opponent,
                    )
                }
            }
            val tags = CardIntentAnalyzer.analyze(probe).tags
            tags shouldNotContain IntentTag.GAME_WIN
            tags shouldContain IntentTag.DRAW
        }

        test("a punisher rates as its weaker branch, and face damage rates as a clock") {
            fun instant(name: String, body: com.wingedsheep.sdk.dsl.CardBuilder.() -> Unit) = card(name) {
                manaCost = "{2}{R}"
                typeLine = "Instant"
                body()
            }
            val punisher = instant("Punisher Rating Probe") {
                spell {
                    val opponent = target(Targets.Opponent)
                    effect = Effects.May(
                        effect = Effects.DealDamage(4, opponent),
                        otherwise = Effects.DrawCards(3),
                        decisionMaker = opponent,
                    )
                }
            }
            val drawThree = instant("Draw Three Rating Probe") { spell { effect = Effects.DrawCards(3) } }
            val faceBurn = instant("Face Burn Rating Probe") {
                spell { effect = Effects.DealDamage(4, target(Targets.Opponent)) }
            }
            val creatureBurn = instant("Creature Burn Rating Probe") {
                spell { effect = Effects.DealDamage(4, target(TargetFilter.Creature)) }
            }
            fun rating(card: com.wingedsheep.sdk.model.CardDefinition) = LimitedCardRater.explain(card).heuristicRating
            withClue("the opponent takes the four damage rather than give us three cards") {
                rating(punisher) shouldBeLessThan rating(drawThree)
            }
            withClue("four to the face is no answer to a creature") {
                rating(faceBurn) shouldBeLessThan rating(creatureBurn)
            }
        }

        test("Risk Factor is cashed in the opponent's end step, like any instant that draws") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(1, "Risk Factor")
                .build()
                .advanceToPriority(1, Step.END)
            val policy = HoldPolicy(IntentCatalog.of(cardRegistry), cashCantripsInTheEndStep = true)
            policy.verdictFor(
                game.state, game.player1Id, "Risk Factor",
                cast = CastSpell(game.player1Id, game.findCardsInHand(1, "Risk Factor").first()),
            ).shouldBeInstanceOf<TimingVerdict.Adjust>()
        }

        test("a punisher the targeted opponent decides aims at that opponent") {
            // Combustible Gearhulk: "target opponent may have you draw three cards. If the player
            // doesn't, you mill three cards, then this deals damage to that player …". The draw
            // branch says nothing of the slot; the other hits it, so the slot is harmful.
            val trigger = card("Combustible Gearhulk").script.triggeredAbilities.single()
            val reqs = listOfNotNull(trigger.targetRequirement) + trigger.additionalTargetRequirements
            TargetPolarityAnalyzer.forRequirements(trigger.effect, reqs) shouldBe listOf(TargetPolarity.HARMFUL)
            withClue("Risk Factor: four damage to the target, or three cards for us") {
                TargetPolarityAnalyzer.forSpell(card("Risk Factor")) shouldBe listOf(TargetPolarity.HARMFUL)
            }
        }

        test("Clash of Elements is not cast at our own creature when theirs can't be targeted") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Gladecover Scout")
                .withCardInHand(1, "Clash of Elements")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Clash of Elements"
            }

            withClue("the hexproof Scout leaves only our Bears to target, and either branch tucks it") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry)) shouldBe true
            }
        }
    }
}
