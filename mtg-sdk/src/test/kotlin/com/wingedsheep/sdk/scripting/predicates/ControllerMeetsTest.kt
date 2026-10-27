package com.wingedsheep.sdk.scripting.predicates

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.serialization.json.Json

class ControllerMeetsTest : FunSpec({
    test("a you-control condition is re-voiced for the candidate's controller") {
        StatePredicate.ControllerMeets(
            Conditions.YouControl(GameObjectFilter.Land.withSubtype(Subtype.ISLAND))
        ).description shouldBe "whose controller controls an Island land"
    }

    test("a count condition reads about their graveyard, never yours, and drops the leading if") {
        val description = StatePredicate.ControllerMeets(Conditions.CardsInGraveyardAtLeast(8)).description
        description shouldStartWith "whose controller "
        description shouldNotContain "your "
        description shouldNotContain " if "
    }

    test("round-trips through serialization") {
        val predicate: StatePredicate = StatePredicate.ControllerMeets(Conditions.CardsInGraveyardAtLeast(8))
        Json.decodeFromString<StatePredicate>(Json.encodeToString(predicate)) shouldBe predicate
    }
})
