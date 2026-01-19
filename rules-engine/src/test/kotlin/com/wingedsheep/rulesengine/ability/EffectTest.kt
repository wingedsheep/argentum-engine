package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class EffectTest : FunSpec({

    context("Effect Operators") {
        test("then operator creates CompositeEffect") {
            val effect1 = GainLifeEffect(1)
            val effect2 = DrawCardsEffect(1)

            val composite = effect1 then effect2

            composite.shouldBeInstanceOf<CompositeEffect>()
            composite.effects shouldHaveSize 2
            composite.effects[0] shouldBe effect1
            composite.effects[1] shouldBe effect2
        }

        test("then operator flattens nested CompositeEffects") {
            val effect1 = GainLifeEffect(1)
            val effect2 = DrawCardsEffect(1)
            val effect3 = LoseLifeEffect(1)

            // (A then B) then C
            val composite1 = effect1 then effect2
            val finalComposite = composite1 then effect3

            finalComposite.effects shouldHaveSize 3
            finalComposite.effects[0] shouldBe effect1
            finalComposite.effects[1] shouldBe effect2
            finalComposite.effects[2] shouldBe effect3
        }
    }

    context("GainLifeEffect") {
        test("controller gain life has correct description") {
            val effect = GainLifeEffect(amount = 3, target = EffectTarget.Controller)
            effect.description shouldBe "You gain 3 life"
        }

        test("opponent gain life has correct description") {
            val effect = GainLifeEffect(amount = 2, target = EffectTarget.Opponent)
            effect.description shouldBe "Target opponent gains 2 life"
        }
    }

    context("LoseLifeEffect") {
        test("opponent lose life has correct description") {
            val effect = LoseLifeEffect(amount = 3, target = EffectTarget.Opponent)
            effect.description shouldBe "Target opponent loses 3 life"
        }

        test("controller lose life has correct description") {
            val effect = LoseLifeEffect(amount = 2, target = EffectTarget.Controller)
            effect.description shouldBe "You lose 2 life"
        }
    }

    context("DealDamageEffect") {
        test("damage to target creature has correct description") {
            val effect = DealDamageEffect(amount = 3, target = EffectTarget.TargetCreature)
            effect.description shouldBe "Deal 3 damage to target creature"
        }

        test("damage to any target has correct description") {
            val effect = DealDamageEffect(amount = 2, target = EffectTarget.AnyTarget)
            effect.description shouldBe "Deal 2 damage to any target"
        }

        test("damage to each opponent has correct description") {
            val effect = DealDamageEffect(amount = 1, target = EffectTarget.EachOpponent)
            effect.description shouldBe "Deal 1 damage to each opponent"
        }
    }

    context("DrawCardsEffect") {
        test("draw a card has correct description") {
            val effect = DrawCardsEffect(count = 1, target = EffectTarget.Controller)
            effect.description shouldBe "Draw a card"
        }

        test("draw multiple cards has correct description") {
            val effect = DrawCardsEffect(count = 3, target = EffectTarget.Controller)
            effect.description shouldBe "Draw 3 cards"
        }

        test("opponent draws has correct description") {
            val effect = DrawCardsEffect(count = 2, target = EffectTarget.Opponent)
            effect.description shouldBe "Target opponent draws 2 cards"
        }
    }

    context("DiscardCardsEffect") {
        test("discard a card has correct description") {
            val effect = DiscardCardsEffect(count = 1, target = EffectTarget.Opponent)
            effect.description shouldBe "Target opponent discards a card"
        }

        test("discard multiple cards has correct description") {
            val effect = DiscardCardsEffect(count = 2, target = EffectTarget.Opponent)
            effect.description shouldBe "Target opponent discards 2 cards"
        }
    }

    context("DestroyEffect") {
        test("destroy target creature has correct description") {
            val effect = DestroyEffect(target = EffectTarget.TargetCreature)
            effect.description shouldBe "Destroy target creature"
        }

        test("destroy target permanent has correct description") {
            val effect = DestroyEffect(target = EffectTarget.TargetPermanent)
            effect.description shouldBe "Destroy target permanent"
        }
    }

    context("ModifyStatsEffect") {
        test("positive modifier has correct description") {
            val effect = ModifyStatsEffect(
                powerModifier = 2,
                toughnessModifier = 2,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            )
            effect.description shouldBe "target creature gets +2/+2 until end of turn"
        }

        test("negative modifier has correct description") {
            val effect = ModifyStatsEffect(
                powerModifier = -1,
                toughnessModifier = -1,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            )
            effect.description shouldBe "target creature gets -1/-1 until end of turn"
        }

        test("mixed modifiers has correct description") {
            val effect = ModifyStatsEffect(
                powerModifier = 3,
                toughnessModifier = -1,
                target = EffectTarget.TargetCreature,
                untilEndOfTurn = true
            )
            effect.description shouldBe "target creature gets +3/-1 until end of turn"
        }
    }

    context("AddCountersEffect") {
        test("single counter has correct description") {
            val effect = AddCountersEffect(
                counterType = "+1/+1",
                count = 1,
                target = EffectTarget.TargetCreature
            )
            effect.description shouldBe "Put 1 +1/+1 counter on target creature"
        }

        test("multiple counters has correct description") {
            val effect = AddCountersEffect(
                counterType = "+1/+1",
                count = 3,
                target = EffectTarget.Self
            )
            effect.description shouldBe "Put 3 +1/+1 counters on this creature"
        }
    }

    context("AddManaEffect") {
        test("add green mana has correct description") {
            val effect = AddManaEffect(color = Color.GREEN, amount = 1)
            effect.description shouldBe "Add {G}"
        }

        test("add multiple red mana has correct description") {
            val effect = AddManaEffect(color = Color.RED, amount = 2)
            effect.description shouldBe "Add {R}{R}"
        }
    }

    context("CreateTokenEffect") {
        test("soldier token has correct description") {
            val effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier")
            )
            effect.description shouldContain "1/1"
            effect.description shouldContain "white"
            effect.description shouldContain "Soldier"
            effect.description shouldContain "creature token"
        }

        test("multiple tokens has correct description") {
            val effect = CreateTokenEffect(
                count = 2,
                power = 2,
                toughness = 2,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Beast")
            )
            effect.description shouldContain "2 2/2"
            effect.description shouldContain "creature tokens"
        }

        test("token with keywords has correct description") {
            val effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Spirit"),
                keywords = setOf(Keyword.FLYING)
            )
            effect.description shouldContain "flying"
        }
    }

    context("CompositeEffect") {
        test("combines multiple effects") {
            val effect = CompositeEffect(
                effects = listOf(
                    GainLifeEffect(amount = 2, target = EffectTarget.Controller),
                    DrawCardsEffect(count = 1, target = EffectTarget.Controller)
                )
            )
            effect.description shouldContain "You gain 2 life"
            effect.description shouldContain "Draw a card"
        }
    }

    context("EffectTarget") {
        test("Controller has correct description") {
            EffectTarget.Controller.description shouldBe "you"
        }

        test("Opponent has correct description") {
            EffectTarget.Opponent.description shouldBe "target opponent"
        }

        test("TargetCreature has correct description") {
            EffectTarget.TargetCreature.description shouldBe "target creature"
        }

        test("AnyTarget has correct description") {
            EffectTarget.AnyTarget.description shouldBe "any target"
        }

        test("AllCreatures has correct description") {
            EffectTarget.AllCreatures.description shouldBe "all creatures"
        }

        test("EachOpponent has correct description") {
            EffectTarget.EachOpponent.description shouldBe "each opponent"
        }
    }
})
