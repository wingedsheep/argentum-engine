package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.BeatrixLoyalGeneral
import com.wingedsheep.mtg.sets.definitions.lea.cards.WallOfWood
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Equipment moved without paying equip — "attach any number of Equipment you control to target
 * creature you control" (Beatrix, Loyal General), "attach target Equipment you control to target
 * creature you control" (Halvar, Weapons Vendor) — reads as [IntentTag.EQUIPMENT_RELOCATION], its
 * creature slot as a gift, and the card as asking the deck for Equipment.
 *
 * The decision it changes: Beatrix's beginning-of-combat trigger used to take the removal ranking
 * for its unread "target creature you control" — worst creature first — and aimed the Equipment at a
 * 0/3 defender. Read as a gift, it picks the creature that attacks.
 */
class EquipmentRelocationReadingAiTest : ScenarioTestBase() {

    private fun tags(card: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(card)).tags

    init {
        test("free equips read as Equipment relocation") {
            for (card in listOf("Beatrix, Loyal General", "Halvar, God of Battle", "Weapons Vendor", "Vulshok Battlemaster")) {
                withClue(card) { tags(card) shouldContain IntentTag.EQUIPMENT_RELOCATION }
            }
        }

        test("the creature slot is a gift, and an unreadable Equipment's equip slot is left to the simulation") {
            val beatrix = cardRegistry.requireCard("Beatrix, Loyal General").script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(beatrix.effect, beatrix.allTargetRequirements) shouldBe
                listOf(TargetPolarity.BENEFICIAL)
            val blade = cardRegistry.requireCard("Blade of Shared Souls")
            val equip = blade.script.activatedAbilities.single()
            TargetPolarityAnalyzer.forAbility(blade, equip) shouldBe
                listOf(TargetPolarity.FLEXIBLE)
        }

        test("Beatrix reads fully") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Beatrix, Loyal General"))
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("a free equip asks the deck for Equipment; attaching a stolen one does not") {
            val equipment = { card: String ->
                SynergyAxes.of(cardRegistry.requireCard(card)).asks
                    .any { ask -> ask.filter == GameObjectFilter.Artifact.withSubtype("Equipment") }
            }
            withClue("Beatrix") { equipment("Beatrix, Loyal General") shouldBe true }
            withClue("Stolen Uniform") { equipment("Stolen Uniform") shouldBe false }
        }

        test("Beatrix moves the Equipment onto the creature that attacks, not the wall") {
            val cards = TestCards.all + listOf(BeatrixLoyalGeneral, WallOfWood, Bonesplitter)
            val registry = CardRegistry().apply { register(cards) }
            val d = GameTestDriver().apply {
                registerCards(cards)
                initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            val beatrix = d.putCreatureOnBattlefield(d.player1, "Beatrix, Loyal General")
            d.removeSummoningSickness(beatrix)
            d.putCreatureOnBattlefield(d.player1, "Wall of Wood")
            d.putPermanentOnBattlefield(d.player1, "Bonesplitter")
            var decision: ChooseTargetsDecision? = null
            repeat(10) {
                val pending = d.state.pendingDecision
                if (pending is ChooseTargetsDecision && pending.playerId == d.player1) {
                    decision = pending
                    return@repeat
                }
                if (decision == null) {
                    if (pending != null) d.autoResolveDecision() else d.bothPass()
                }
            }
            val targets = decision ?: error("Beatrix's beginning-of-combat target decision never arrived")
            val response = AIPlayer.create(registry, d.player1, AiProfile.LIVE).respondToDecision(d.state, targets)
            response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.getValue(0) shouldContainExactly listOf(beatrix)
        }
    }
}
