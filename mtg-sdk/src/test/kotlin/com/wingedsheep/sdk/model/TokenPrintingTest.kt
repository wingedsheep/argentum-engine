package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Matching rules for a set's token art, including the several-arts-for-one-token case that lets a
 * batch of tokens created at once show a whole run of printed illustrations.
 */
class TokenPrintingTest : FunSpec({

    fun dog(art: String) = TokenPrinting(
        name = "Dog",
        imageUri = art,
        power = 1,
        toughness = 1,
        colors = setOf(Color.WHITE),
    )

    val dogs = listOf(dog("dog1"), dog("dog2"), dog("dog3"), dog("dog4"))

    test("every row printed for one token identity is returned, in declaration order") {
        TokenPrinting.allMatches(dogs, "Dog", 1, 1, setOf(Color.WHITE))
            .map { it.imageUri } shouldBe listOf("dog1", "dog2", "dog3", "dog4")
    }

    test("bestMatch stays the first of those, for callers that only need one") {
        TokenPrinting.bestMatch(dogs, "Dog", 1, 1, setOf(Color.WHITE))?.imageUri shouldBe "dog1"
    }

    test("a pinned identity wins outright — a same-name row for another token is not in the run") {
        // Wand of the Elements' two Elementals: matching on name alone would deal the red art out
        // to blue Elementals as well.
        val elementals = listOf(
            TokenPrinting("Elemental", "blue", power = 2, toughness = 2, colors = setOf(Color.BLUE)),
            TokenPrinting("Elemental", "red", power = 3, toughness = 3, colors = setOf(Color.RED)),
        )

        TokenPrinting.allMatches(elementals, "Elemental", 2, 2, setOf(Color.BLUE))
            .map { it.imageUri } shouldBe listOf("blue")
        TokenPrinting.allMatches(elementals, "Elemental", 3, 3, setOf(Color.RED))
            .map { it.imageUri } shouldBe listOf("red")
    }

    test("name-only rows stand in when nothing pins the identity") {
        // A synced row pins the P/T of the *printed* token; the engine's can legitimately differ
        // (a variable-P/T printing, a colour granted rather than printed). The set's art for the
        // right token name still beats engine-wide generic art.
        val printed = listOf(TokenPrinting("Wurm", "wurm", power = 6, toughness = 6, colors = setOf(Color.GREEN)))

        TokenPrinting.allMatches(printed, "Wurm", 3, 3, setOf(Color.GREEN))
            .map { it.imageUri } shouldBe listOf("wurm")
    }

    test("a token the set never printed matches nothing") {
        TokenPrinting.allMatches(dogs, "Cat", 1, 1, setOf(Color.WHITE)).shouldBeEmpty()
        TokenPrinting.bestMatch(dogs, "Cat", 1, 1, setOf(Color.WHITE)) shouldBe null
    }

    test("a bare row matches every token of that name the set mints") {
        val treasure = listOf(TokenPrinting("Treasure", "treasure"))

        TokenPrinting.allMatches(treasure, "Treasure").map { it.imageUri } shouldBe listOf("treasure")
    }

    test("names match across word order, since a token name is joined from an unordered set") {
        val army = listOf(TokenPrinting("Zombie Army", "army"))

        TokenPrinting.allMatches(army, "Army Zombie").map { it.imageUri } shouldBe listOf("army")
    }
})
