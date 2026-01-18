package com.wingedsheep.rulesengine.player

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.assertions.throwables.shouldThrow

class ManaPoolTest : FunSpec({

    context("creation") {
        test("EMPTY pool has no mana") {
            val pool = ManaPool.EMPTY
            pool.isEmpty.shouldBeTrue()
            pool.total shouldBe 0
        }

        test("of factory creates pool with specified mana") {
            val pool = ManaPool.of(white = 2, blue = 1, red = 3)
            pool.white shouldBe 2
            pool.blue shouldBe 1
            pool.red shouldBe 3
            pool.black shouldBe 0
            pool.green shouldBe 0
            pool.colorless shouldBe 0
            pool.total shouldBe 6
        }
    }

    context("adding mana") {
        test("add colored mana") {
            val pool = ManaPool.EMPTY
                .add(Color.WHITE)
                .add(Color.WHITE)
                .add(Color.BLUE)

            pool.white shouldBe 2
            pool.blue shouldBe 1
            pool.total shouldBe 3
        }

        test("add multiple mana at once") {
            val pool = ManaPool.EMPTY.add(Color.RED, 5)
            pool.red shouldBe 5
        }

        test("add colorless mana") {
            val pool = ManaPool.EMPTY.addColorless(3)
            pool.colorless shouldBe 3
        }
    }

    context("spending mana") {
        test("spend colored mana") {
            val pool = ManaPool.of(white = 3, blue = 2)
                .spend(Color.WHITE, 2)

            pool.white shouldBe 1
            pool.blue shouldBe 2
        }

        test("spend throws when not enough mana") {
            val pool = ManaPool.of(white = 1)

            shouldThrow<IllegalArgumentException> {
                pool.spend(Color.WHITE, 2)
            }
        }

        test("spend colorless mana") {
            val pool = ManaPool.of(colorless = 5).spendColorless(3)
            pool.colorless shouldBe 2
        }

        test("spendGeneric uses colorless first") {
            val pool = ManaPool.of(white = 2, colorless = 3)
                .spendGeneric(4)

            pool.colorless shouldBe 0
            pool.white shouldBe 1
        }

        test("spendGeneric uses colors in WUBRG order after colorless") {
            val pool = ManaPool.of(white = 1, blue = 1, black = 1, red = 1, green = 1)
                .spendGeneric(3)

            pool.white shouldBe 0
            pool.blue shouldBe 0
            pool.black shouldBe 0
            pool.red shouldBe 1
            pool.green shouldBe 1
        }
    }

    context("canPay") {
        test("can pay simple colored cost") {
            val pool = ManaPool.of(white = 2)
            pool.canPay(ManaCost.parse("{W}{W}")).shouldBeTrue()
        }

        test("cannot pay if not enough colored mana") {
            val pool = ManaPool.of(white = 1)
            pool.canPay(ManaCost.parse("{W}{W}")).shouldBeFalse()
        }

        test("can pay mixed cost") {
            val pool = ManaPool.of(white = 2, red = 3)
            pool.canPay(ManaCost.parse("{2}{W}{W}")).shouldBeTrue()
        }

        test("cannot pay mixed cost if not enough total") {
            val pool = ManaPool.of(white = 2, red = 1)
            pool.canPay(ManaCost.parse("{2}{W}{W}")).shouldBeFalse()
        }

        test("can pay zero cost") {
            val pool = ManaPool.EMPTY
            pool.canPay(ManaCost.ZERO).shouldBeTrue()
        }

        test("can pay X cost with any amount") {
            val pool = ManaPool.EMPTY
            pool.canPay(ManaCost.parse("{X}{R}")).shouldBeFalse() // Need red

            val poolWithRed = ManaPool.of(red = 1)
            poolWithRed.canPay(ManaCost.parse("{X}{R}")).shouldBeTrue()
        }
    }

    context("empty") {
        test("empty returns EMPTY pool") {
            val pool = ManaPool.of(white = 5, blue = 3)
            pool.empty() shouldBe ManaPool.EMPTY
        }
    }

    context("toString") {
        test("formats non-empty pool") {
            val pool = ManaPool.of(white = 2, red = 1, colorless = 3)
            pool.toString() shouldBe "2W 1R 3C"
        }

        test("formats empty pool as empty string") {
            ManaPool.EMPTY.toString() shouldBe ""
        }
    }

    context("get by color") {
        test("returns correct amount for each color") {
            val pool = ManaPool.of(white = 1, blue = 2, black = 3, red = 4, green = 5)

            pool.get(Color.WHITE) shouldBe 1
            pool.get(Color.BLUE) shouldBe 2
            pool.get(Color.BLACK) shouldBe 3
            pool.get(Color.RED) shouldBe 4
            pool.get(Color.GREEN) shouldBe 5
        }
    }
})
