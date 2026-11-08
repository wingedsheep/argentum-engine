package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.SynergyPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.RemovalPatience
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A payoff is worth the fuel beside it ([AiProfile.payoffsReadTheirFuel], the `SynergyPresence`
 * term over the structural [SynergyAxes] reading).
 */
class SynergyPresenceAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun axes(name: String) = SynergyAxes.of(intents.definition(name)!!)

    private fun sacrificeEngine(): TestGame = scenario().withPlayers()
        .withTurnNumber(6)
        .withActivePlayer(1)
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardInHand(1, "Eviscerate")
        .withCardOnBattlefield(2, "Blood Artist")
        .withCardOnBattlefield(2, "Viscera Seer")
        .withCardOnBattlefield(2, "Doomed Dissenter")
        .withCardOnBattlefield(2, "Doomed Dissenter")
        .withCardOnBattlefield(2, "Doomed Dissenter")
        .withCardOnBattlefield(2, "Doomed Dissenter")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    /** The permanent [profile]'s AI aims its removal at, or what it did instead. */
    private fun TestGame.removalTarget(profile: AiProfile): String {
        val action = AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)
        val cast = action as? CastSpell ?: return "no cast: $action"
        val id = (cast.targets.single() as ChosenTarget.Permanent).entityId
        return state.getEntity(id)?.get<CardComponent>()?.name ?: "?"
    }

    init {
        test("the reader finds payoffs on their triggers and feeders on their effects, costs and keywords") {
            // Payoffs: "whenever ... another creature dies", "whenever you gain life",
            // "whenever another creature enters", prowess, "whenever you cast an instant or sorcery".
            axes("Blood Artist").rewards shouldContain SynergyAxis.DEATH
            axes("Zulaport Cutthroat").rewards shouldContain SynergyAxis.DEATH
            axes("Ajani's Pridemate").rewards shouldContain SynergyAxis.LIFEGAIN
            axes("Soul Warden").rewards shouldContain SynergyAxis.CREATURES_ENTER
            axes("Monastery Swiftspear").rewards shouldContain SynergyAxis.SPELLS
            axes("Young Pyromancer").rewards shouldContain SynergyAxis.SPELLS
            // "Whenever a spell or ability you control counters a spell" cashes in counterspells.
            axes("Baral, Chief of Compliance").rewards shouldContain SynergyAxis.SPELLS

            // Feeders: an activated sacrifice outlet, a sacrifice as an additional cost, creature tokens,
            // lifelink, life gain on an effect, an instant.
            axes("Viscera Seer").feeds shouldContain SynergyAxis.DEATH
            axes("Village Rites").feeds shouldContain SynergyAxis.DEATH
            axes("Raise the Alarm").feeds shouldContainAll
                setOf(SynergyAxis.TOKENS, SynergyAxis.CREATURES_ENTER, SynergyAxis.DEATH)
            axes("Vampire Nighthawk").feeds shouldContain SynergyAxis.LIFEGAIN
            axes("Healer's Hawk").feeds shouldContain SynergyAxis.LIFEGAIN
            axes("Lightning Bolt").feeds shouldContain SynergyAxis.SPELLS
            axes("Young Pyromancer").feeds shouldContain SynergyAxis.TOKENS

            // "When this dies" is the card's own death, not a payoff; a vanilla creature rewards nothing.
            axes("Doomed Dissenter").rewards shouldNotContain SynergyAxis.DEATH
            axes("Grizzly Bears").rewards shouldBe emptySet()
        }

        test("a feeding effect inside a ForEach body is read: a counter on each creature feeds +1/+1 counters") {
            // Bellowing Aegisaur's enrage and Victory's Envoy's upkeep trigger put a +1/+1 counter
            // on each creature through ForEachInGroup; the effect walk stops at ForEach bodies.
            axes("Bellowing Aegisaur").feeds shouldContain SynergyAxis.PLUS_COUNTERS
            axes("Victory's Envoy").feeds shouldContain SynergyAxis.PLUS_COUNTERS
        }

        test("a payoff's term grows with the fuel beside it, and only our own hand is read") {
            val empty = scenario().withPlayers()
                .withCardOnBattlefield(1, "Blood Artist")
                .withCardInHand(2, "Village Rites")
                .build()
            val fuelled = scenario().withPlayers()
                .withCardOnBattlefield(1, "Blood Artist")
                .withCardOnBattlefield(1, "Viscera Seer")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Village Rites")
                .build()
            val term = SynergyPresence(intents)
            fun value(game: TestGame, player: com.wingedsheep.sdk.model.EntityId, ownSeat: Boolean) =
                term.value(game.state, game.state.projectedState, player, ownSeat)

            // Alone, Blood Artist is a 0/1: nothing to cash in.
            value(empty, empty.player1Id, ownSeat = true) shouldBe 0.0
            // Two other creatures to die, plus half a unit for the outlet in hand.
            value(fuelled, fuelled.player1Id, ownSeat = true) shouldBe
                SynergyPresence.PAIR_VALUE * (2 + SynergyPresence.HAND_SHARE)
            // Read as an opponent, the hand is hidden: only the board counts.
            value(fuelled, fuelled.player1Id, ownSeat = false) shouldBe SynergyPresence.PAIR_VALUE * 2
        }

        // Eviscerate in our main phase against a fed sacrifice engine: Blood Artist, a Viscera Seer
        // to sacrifice with and four Doomed Dissensters to sacrifice — five fuel, past the cap. On
        // bodies alone the 0/1 is the worst kill on the board; priced as the engine it is, it is the
        // one card whose loss stops every drain, while killing one of the 1/1s leaves it fully fed.
        // The removal-patience bar is off on both sides so this pins the target, not the timing.
        test("removal aims at the engine, not the bodies feeding it") {
            val game = sacrificeEngine()
            val base = AiProfile.LIVE.copy(holdRemovalForBetterTargets = false)
            val without = game.removalTarget(base.copy(payoffsReadTheirFuel = false))
            val with = game.removalTarget(base)
            withClue("with the term: $with; without: $without") {
                without shouldNotBe "Blood Artist"
                with shouldBe "Blood Artist"
            }
        }

        // The removal-patience bar holds a four-mana removal spell for a target worth four mana. A
        // 0/1 is far below it on its body; the fuel it cashes in is what an engine is worth.
        test("an engine clears more of the removal bar than its body does") {
            val game = sacrificeEngine()
            val artist = game.findPermanent("Blood Artist")!!
            val eviscerate = game.state.getZone(game.player1Id, com.wingedsheep.sdk.core.Zone.HAND).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Eviscerate"
            }
            val card = game.state.getEntity(eviscerate)!!.get<CardComponent>()!!
            fun discount(synergy: SynergyPresence?) = RemovalPatience.discount(
                game.state, game.player1Id, intents.forName("Eviscerate")!!, card,
                listOf(ChosenTarget.Permanent(artist)), boardPresenceWeight = 1.5, synergy = synergy,
            )
            discount(null) shouldBeGreaterThan discount(SynergyPresence(intents))
        }

        test("the opponent's assembled engine scores against us") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Blood Artist")
                .withCardOnBattlefield(2, "Viscera Seer")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val score = SynergyPresence(intents).score(game.state, game.state.projectedState, game.player1Id)
            (-score) shouldBeGreaterThan 0.0
        }
    }
}
