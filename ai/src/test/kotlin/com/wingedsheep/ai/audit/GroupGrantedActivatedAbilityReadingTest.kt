package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * An activated ability handed to a group of our creatures — Gruesome Slaughter's "until end of turn,
 * colorless creatures you control gain '{T}: This creature deals damage equal to its power to target
 * creature'" — reads as what the same ability printed on each of them does, as the
 * single-permanent grant (an Equipment's) already does. A grant to an opponent's creatures is theirs.
 */
class GroupGrantedActivatedAbilityReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun named(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("a group granted a tap-to-damage-a-creature ability reads as removal") {
        val slaughter = named("Gruesome Slaughter")
        AiReadingAudit.read(slaughter).readFully shouldBe true
        CardIntentAnalyzer.analyze(slaughter).tags shouldContain IntentTag.REMOVAL
    }

    test("the same ability granted to an opponent's creatures is theirs, and reads as nothing of ours") {
        val gift = card("Opponent Grant Probe") {
            manaCost = "{6}"
            typeLine = "Sorcery"
            spell {
                effect = Effects.GrantActivatedAbilityToGroup(
                    ability = grantedActivatedAbility {
                        cost = Costs.Tap
                        val creature = target(TargetFilter.Creature)
                        effect = Effects.DealDamage(DynamicAmounts.sourcePower(), creature, damageSource = EffectTarget.Self)
                    },
                    filter = GroupFilter(GameObjectFilter.Creature.opponentControls()),
                    duration = Duration.EndOfTurn,
                )
            }
        }
        CardIntentAnalyzer.analyze(gift).tags shouldNotContain IntentTag.REMOVAL
    }
})
