package com.wingedsheep.engine.registry

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Overlay semantics: a registry built with a `parent` resolves its own entries first and defers the
 * rest, which is how a replay stacks the card definitions it was recorded with over the live corpus
 * (`ReplayCardPin`). The interesting cases are all about *partial* shadowing — an overlay pins the
 * handful of cards a game touched, so almost every lookup has to fall through, and the ones that
 * don't must not drag the parent's stale answer along with them.
 */
class CardRegistryTest : FunSpec({

    fun bear(name: String, power: Int, collectorNumber: String? = null) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = power,
        toughness = power,
        metadata = ScryfallMetadata(collectorNumber = collectorNumber),
    )

    fun forest(collectorNumber: String? = null) = CardDefinition.basicLand(
        name = "Forest",
        subtype = Subtype.FOREST,
        metadata = ScryfallMetadata(collectorNumber = collectorNumber),
    )

    fun power(card: CardDefinition?) = card?.creatureStats?.power

    test("an overlay shadows the parent by name and falls through for everything else") {
        val live = CardRegistry().apply { register(listOf(bear("Grizzly Bears", 5), forest())) }
        val overlay = CardRegistry(parent = live).apply { register(bear("Grizzly Bears", 2)) }

        power(overlay.getCard("Grizzly Bears")) shouldBe CharacteristicValue.Fixed(2)
        overlay.getCard("Forest")?.name shouldBe "Forest"
        overlay.getCard("Nonexistent Card").shouldBeNull()
        overlay.hasCard("Forest") shouldBe true
    }

    test("registering into an overlay never mutates the parent") {
        val live = CardRegistry().apply { register(bear("Grizzly Bears", 5)) }
        CardRegistry(parent = live).apply { register(listOf(bear("Grizzly Bears", 2), bear("Pin Bear", 1))) }

        power(live.getCard("Grizzly Bears")) shouldBe CharacteristicValue.Fixed(5)
        live.getCard("Pin Bear").shouldBeNull()
        live.size shouldBe 1
    }

    test("size and allCardNames report the union, counting a shadowed name once") {
        val live = CardRegistry().apply { register(listOf(bear("Grizzly Bears", 5), forest())) }
        val overlay = CardRegistry(parent = live).apply {
            register(listOf(bear("Grizzly Bears", 2), bear("Pin Bear", 1)))
        }

        overlay.allCardNames() shouldContainExactlyInAnyOrder listOf("Grizzly Bears", "Forest", "Pin Bear")
        overlay.size shouldBe 3
        live.size shouldBe 2
    }

    test("a name is a land name if either layer says so, and shadowing can take it away") {
        val live = CardRegistry().apply { register(listOf(forest(), bear("Grizzly Bears", 5))) }

        CardRegistry(parent = live).apply { register(bear("Pin Bear", 1)) }
            .landCardNames() shouldContainExactly listOf("Forest")

        // The overlay redefines Forest as a creature: the parent's land entry is shadowed, not merged.
        CardRegistry(parent = live).apply { register(bear("Forest", 1)) }
            .landCardNames() shouldBe emptySet()
    }

    test("getCardsByName keeps parent printings the overlay never pinned, overlay first") {
        val live = CardRegistry().apply { register(listOf(forest("196"), forest("197"))) }
        val overlay = CardRegistry(parent = live).apply { register(forest("196")) }

        val variants = overlay.getCardsByName("Forest")
        variants.map { it.metadata.collectorNumber } shouldContainExactly listOf("196", "197")
        // The pinned 196 is the overlay's own object, not the parent's.
        (variants[0] === overlay.getCard("Forest#196")) shouldBe true
    }

    test("a root registry is unaffected by the parent-aware branches") {
        val live = CardRegistry().apply { register(listOf(bear("Grizzly Bears", 5), forest())) }

        live.size shouldBe 2
        live.allCardNames() shouldContainExactlyInAnyOrder listOf("Grizzly Bears", "Forest")
        live.landCardNames() shouldContainExactly listOf("Forest")
        live.getCard("Grizzly Bears")?.name shouldBe "Grizzly Bears"
    }
})
