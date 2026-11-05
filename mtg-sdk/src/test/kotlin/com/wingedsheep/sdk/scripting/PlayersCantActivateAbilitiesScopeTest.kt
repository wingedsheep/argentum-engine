package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [ActivatedAbilityScope] — which ability kinds a [PlayersCantActivateAbilities] prohibition
 * forbids — and the wordings it renders.
 */
class PlayersCantActivateAbilitiesScopeTest : DescribeSpec({

    describe("covers(isManaAbility, isLoyaltyAbility)") {
        // (mana, loyalty) per ability kind: an ordinary ability, a mana ability, a loyalty ability.
        val kinds = listOf(false to false, true to false, false to true)
        fun row(scope: ActivatedAbilityScope) = kinds.map { (m, l) -> scope.covers(m, l) }

        it("ALL forbids every kind") { row(ActivatedAbilityScope.ALL) shouldBe listOf(true, true, true) }
        it("NON_MANA exempts mana abilities") { row(ActivatedAbilityScope.NON_MANA) shouldBe listOf(true, false, true) }
        it("LOYALTY forbids only loyalty abilities") { row(ActivatedAbilityScope.LOYALTY) shouldBe listOf(false, false, true) }
        it("NON_MANA_NON_LOYALTY exempts mana and loyalty abilities") {
            row(ActivatedAbilityScope.NON_MANA_NON_LOYALTY) shouldBe listOf(true, false, false)
        }
    }

    describe("description") {
        it("renders the enchanted-player wording") {
            PlayersCantActivateAbilities(
                affected = Player.EnchantedPlayer,
                abilities = ActivatedAbilityScope.NON_MANA_NON_LOYALTY,
                anyZone = true
            ).description shouldBe
                "enchanted player can't activate abilities that aren't mana abilities or loyalty abilities"
        }
        it("keeps the filtered loyalty wording") {
            PlayersCantActivateAbilities(
                affected = Player.Each,
                permanentFilter = GameObjectFilter.Planeswalker,
                abilities = ActivatedAbilityScope.LOYALTY
            ).description shouldBe
                "${Player.Each.description} can't activate loyalty abilities of ${GameObjectFilter.Planeswalker.description}"
        }
    }

    it("round-trips through serialization") {
        val ability: StaticAbility = PlayersCantActivateAbilities(
            affected = Player.EnchantedPlayer,
            abilities = ActivatedAbilityScope.NON_MANA_NON_LOYALTY,
            anyZone = true
        )
        Json.decodeFromString<StaticAbility>(Json.encodeToString(ability)) shouldBe ability
    }
})
