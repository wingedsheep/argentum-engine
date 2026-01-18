package com.wingedsheep.rulesengine.ecs.layers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class LayerTest : FunSpec({

    context("layer ordering") {
        test("layers are in correct order") {
            val ordered = Layer.inOrder()

            ordered[0] shouldBe Layer.COPY
            ordered[1] shouldBe Layer.CONTROL
            ordered[2] shouldBe Layer.TEXT
            ordered[3] shouldBe Layer.TYPE
            ordered[4] shouldBe Layer.COLOR
            ordered[5] shouldBe Layer.ABILITY
            ordered[6] shouldBe Layer.PT_CDA
            ordered[7] shouldBe Layer.PT_SET
            ordered[8] shouldBe Layer.PT_MODIFY
            ordered[9] shouldBe Layer.PT_COUNTERS
            ordered[10] shouldBe Layer.PT_SWITCH
        }

        test("COPY comes before all other layers") {
            Layer.COPY.isBefore(Layer.CONTROL).shouldBeTrue()
            Layer.COPY.isBefore(Layer.PT_SWITCH).shouldBeTrue()
        }

        test("PT_SWITCH comes after all other layers") {
            Layer.PT_SWITCH.isAfter(Layer.COPY).shouldBeTrue()
            Layer.PT_SWITCH.isAfter(Layer.PT_COUNTERS).shouldBeTrue()
        }

        test("isBefore and isAfter are consistent") {
            Layer.CONTROL.isBefore(Layer.TYPE).shouldBeTrue()
            Layer.TYPE.isAfter(Layer.CONTROL).shouldBeTrue()

            Layer.CONTROL.isAfter(Layer.TYPE).shouldBeFalse()
            Layer.TYPE.isBefore(Layer.CONTROL).shouldBeFalse()
        }
    }

    context("P/T sublayers") {
        test("ptLayers returns all P/T sublayers") {
            val ptLayers = Layer.ptLayers()

            ptLayers.size shouldBe 5
            ptLayers.contains(Layer.PT_CDA).shouldBeTrue()
            ptLayers.contains(Layer.PT_SET).shouldBeTrue()
            ptLayers.contains(Layer.PT_MODIFY).shouldBeTrue()
            ptLayers.contains(Layer.PT_COUNTERS).shouldBeTrue()
            ptLayers.contains(Layer.PT_SWITCH).shouldBeTrue()
        }

        test("isPTLayer identifies P/T layers correctly") {
            Layer.PT_CDA.isPTLayer.shouldBeTrue()
            Layer.PT_SET.isPTLayer.shouldBeTrue()
            Layer.PT_MODIFY.isPTLayer.shouldBeTrue()
            Layer.PT_COUNTERS.isPTLayer.shouldBeTrue()
            Layer.PT_SWITCH.isPTLayer.shouldBeTrue()

            Layer.COPY.isPTLayer.shouldBeFalse()
            Layer.CONTROL.isPTLayer.shouldBeFalse()
            Layer.ABILITY.isPTLayer.shouldBeFalse()
        }

        test("P/T sublayers are in correct relative order") {
            Layer.PT_CDA.isBefore(Layer.PT_SET).shouldBeTrue()
            Layer.PT_SET.isBefore(Layer.PT_MODIFY).shouldBeTrue()
            Layer.PT_MODIFY.isBefore(Layer.PT_COUNTERS).shouldBeTrue()
            Layer.PT_COUNTERS.isBefore(Layer.PT_SWITCH).shouldBeTrue()
        }
    }

    context("layer descriptions") {
        test("all layers have descriptions") {
            Layer.entries.forEach { layer ->
                layer.description.isNotBlank().shouldBeTrue()
            }
        }
    }
})
