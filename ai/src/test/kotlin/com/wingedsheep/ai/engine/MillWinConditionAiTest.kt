package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.evaluation.LibraryClock
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MillWinConditionAiTest : ScenarioTestBase() {
    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private fun card(name: String) = cardRegistry.getCard(name)!!

    private fun position(ours: Int = 24, theirs: Int = 24, engine: String? = null, engineOwner: Int = 1, suppressAbilities: Boolean = false): TestGame =
        scenario().withPlayers().withActivePlayer(1).apply {
            repeat(ours) { withCardInLibrary(1, "Island") }
            repeat(theirs) { withCardInLibrary(2, "Island") }
            if (engine != null) withCardOnBattlefield(engineOwner, engine)
            if (suppressAbilities) withCardOnBattlefield(2, "Mill Suppression")
        }.build()

    private fun pressure(game: TestGame): Double =
        LibraryClock.Bound(intents).score(game.state, game.state.projectedState, game.player1Id)

    init {
        cardRegistry.register(com.wingedsheep.sdk.dsl.card("Mill Suppression") {
            typeLine = "Enchantment"
            staticAbility { ability = LoseAllAbilities(GroupFilter.AllCreatures) }
        })
        test("opponent-capable mills have intent and repeatable value, self-mill and ordinary gathers do not") {
            for (name in listOf("Fleet Swallower", "Navigator's Ruin")) {
                val intent = CardIntentAnalyzer.analyze(card(name))
                intent.tags shouldContain IntentTag.MILL
                intent.affectsOpponent shouldBe true
                intent.repeatable shouldBe true
                SynergyAxes.of(card(name)).feeds shouldNotContain SynergyAxis.GRAVEYARD
                LimitedCardRater.explain(card(name)).heuristic.filter { it.part.startsWith("Trigger") }
                    .sumOf { it.value } shouldBeGreaterThan 0.0
            }
            CardIntentAnalyzer.analyze(card("Navigator's Ruin")).staticPriorValue shouldBe 2.0
            CardIntentAnalyzer.analyzeEffect(Patterns.Library.mill(4)).tags shouldNotContain IntentTag.MILL
            for (count in listOf(0, -1)) {
                CardIntentAnalyzer.analyzeEffect(GatherCardsEffect(
                    CardSource.TopOfLibrary(DynamicAmount.Fixed(count), Player.TargetOpponent, isMill = true), "empty"
                )).tags shouldNotContain IntentTag.MILL
            }
            CardIntentAnalyzer.analyzeEffect(GatherCardsEffect(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(4), Player.TargetOpponent), "looked"
            )).tags shouldNotContain IntentTag.MILL
        }

        test("depletion is progress against a live mill engine, well before the last three cards") {
            val before = pressure(position(engine = "Navigator's Ruin"))
            pressure(position(theirs = 20, engine = "Navigator's Ruin")) shouldBeGreaterThan before
            pressure(position(theirs = 12, engine = "Fleet Swallower")) shouldBeGreaterThan before
            // Our library shrinking is no benefit to our own offensive mill engine.
            pressure(position(ours = 20, engine = "Navigator's Ruin")) shouldBe before
            val facing = pressure(position(engine = "Navigator's Ruin", engineOwner = 2))
            facing shouldBe -before
            facing shouldBeGreaterThan pressure(position(ours = 20, engine = "Navigator's Ruin", engineOwner = 2))
        }

        test("ordinary draws and self-mill without an opposing mill engine remain neutral") {
            pressure(position()) shouldBe 0.0
            pressure(position(ours = 12)) shouldBe 0.0
            pressure(position(theirs = 12)) shouldBe 0.0
            // An Adventure mill does not keep threatening the library once its creature is in play.
            pressure(position(theirs = 12, engine = "Merfolk Secretkeeper")) shouldBe 0.0
            LibraryClock.libraryValue(0) shouldBe -60.0
        }

        test("a mill creature is worth more than its stats alone") {
            val game = position(engine = "Fleet Swallower")
            val id = game.state.getBattlefield(game.player1Id).single()
            val component = game.state.getEntity(id)!!.get<CardComponent>()!!
            val plain = BoardPresence.permanentValue(game.state, game.state.projectedState, id, component)
            BoardPresence.permanentValue(game.state, game.state.projectedState, id, component, intents) shouldBe plain + 0.75
        }

        test("a mill creature stripped of its abilities has neither an ability bonus nor library pressure") {
            val game = position(engine = "Fleet Swallower", suppressAbilities = true)
            val id = game.state.getBattlefield(game.player1Id).single()
            val projected = game.state.projectedState
            projected.hasLostAllAbilities(id) shouldBe true
            pressure(game) shouldBe 0.0
            val component = game.state.getEntity(id)!!.get<CardComponent>()!!
            BoardPresence.permanentValue(game.state, projected, id, component, intents) shouldBe
                BoardPresence.permanentValue(game.state, projected, id, component)
        }

        test("a face-down mill creature does not reveal a mill threat or gain its ability bonus") {
            val game = position(engine = "Fleet Swallower")
            val id = game.state.getBattlefield(game.player1Id).single()
            game.state = game.state.updateEntity(id) { it.with(FaceDownComponent) }
            val projected = game.state.projectedState
            pressure(game) shouldBe 0.0
            val component = game.state.getEntity(id)!!.get<CardComponent>()!!
            BoardPresence.permanentValue(game.state, projected, id, component, intents) shouldBe
                BoardPresence.permanentValue(game.state, projected, id, component)
        }

        test("the AI uses a repeatable mill activation while the opposing library is still large") {
            val game = position(engine = "Codex Shredder")
            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .chooseAction(game.state).shouldBeInstanceOf<ActivateAbility>()
            game.state.getEntity(action.sourceId)!!.get<CardComponent>()!!.name shouldBe "Codex Shredder"
        }
    }
}
