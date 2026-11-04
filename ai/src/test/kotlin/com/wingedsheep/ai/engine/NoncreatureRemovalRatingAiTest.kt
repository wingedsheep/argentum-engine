package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.RemovalBreadth
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.definitions.som.cards.SliceInTwain
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

class NoncreatureRemovalRatingAiTest : FunSpec({
    fun removal(name: String, filter: TargetFilter) = card(name) {
        manaCost = "{2}{G}{G}"
        typeLine = "Instant"
        spell {
            val victim = target(filter)
            effect = Effects.Destroy(victim)
        }
        metadata { rarity = Rarity.UNCOMMON }
    }
    val broad = removal("Broad removal rating probe", TargetFilter.Creature)

    fun CardDefinition.summary() = CardSummary(
        name = name,
        manaCost = manaCost.toString(),
        typeLine = typeLine.toString(),
        rarity = metadata.rarity.name,
        oracleText = oracleText,
        setCode = "ZZZ",
    )

    test("drafts broad creature removal over a noncreature cantrip with the same cost and rarity") {
        LimitedCardRater.explain(SliceInTwain).heuristicRating shouldBeLessThan
            LimitedCardRater.explain(broad).heuristicRating
        val registry = CardRegistry().apply { register(listOf(SliceInTwain, broad)) }
        val bot = EngineAiPlayerController(registry, EntityId.of("bot"), { null })
        bot.chooseDraftPick(
            pack = listOf(SliceInTwain.summary(), broad.summary()),
            pickedSoFar = emptyList(),
            packNumber = 1,
            pickNumber = 1,
        ) shouldContainExactly listOf(broad.name)
    }

    test("artifact and enchantment restrictions discount limited ratings without losing answer roles") {
        val narrow = removal("Noncreature removal rating probe", TargetFilter.ArtifactOrEnchantment)
        val slot = EffectWalker.slots(narrow.script).single()
        RemovalBreadth.isNarrow(narrow) shouldBe false
        RemovalBreadth.forLimitedSlot(slot)!! shouldBeLessThan RemovalBreadth.ofSlot(slot)!!
        for (filter in listOf(TargetFilter.Artifact, TargetFilter.Enchantment, TargetFilter.ArtifactOrEnchantment)) {
            val probe = removal("Limited removal $filter", filter)
            LimitedCardRater.explain(probe).heuristicRating shouldBeLessThan
                LimitedCardRater.explain(broad).heuristicRating
        }
    }

    test("noncreature counterspells retain their spell coverage") {
        val counter = card("Noncreature counter rating probe") {
            manaCost = "{1}{U}"
            typeLine = "Instant"
            spell {
                target(TargetFilter.NoncreatureSpellOnStack)
                effect = Effects.CounterSpell()
            }
        }
        val slot = EffectWalker.slots(counter.script).single()
        RemovalBreadth.forLimitedSlot(slot) shouldBe 1.0
        RemovalBreadth.forLimitedSlot(slot) shouldBe RemovalBreadth.ofSlot(slot)
    }

    test("creature removal and unions including all creatures keep their full rating") {
        val mixed = removal("Mixed removal rating probe", TargetFilter.ArtifactCreatureOrEnchantment)
        LimitedCardRater.explain(mixed).heuristicRating shouldBe LimitedCardRater.explain(broad).heuristicRating
        for (probe in listOf(broad, mixed)) {
            val slot = EffectWalker.slots(probe.script).single()
            RemovalBreadth.forLimitedSlot(slot) shouldBe RemovalBreadth.ofSlot(slot)
        }
    }
})
