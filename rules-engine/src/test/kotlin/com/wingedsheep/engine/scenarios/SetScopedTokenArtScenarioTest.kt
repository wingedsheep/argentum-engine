package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.token.TokenArt
import com.wingedsheep.engine.registry.TokenArtRegistry
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fdn.FoundationsSet
import com.wingedsheep.mtg.sets.definitions.jmp.JumpstartSet
import com.wingedsheep.mtg.sets.tokens.TokenArtData
import com.wingedsheep.sdk.core.Color
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * A creature token shows the art of the set that printed the card creating it.
 *
 * Arahbo, the First Fang (Foundations) creates a 1/1 white Cat. Foundations prints its own Cat
 * (Scryfall `tfdn` #1), so that is the art the token must carry — not the engine-wide generic Cat,
 * and not the arbitrary printing the client used to land on by asking Scryfall for a card named
 * "Cat" when the token carried no image at all.
 *
 * The art comes from `FoundationsSet.tokenArt`, resolved through
 * [com.wingedsheep.engine.registry.TokenArtRegistry] — the card's own script says nothing about
 * art, which is what lets a reprint mint its own set's token.
 */
class SetScopedTokenArtScenarioTest : ScenarioTestBase() {

    /** `tfdn` #1 — Foundations' Cat token. */
    private val foundationsCat = "2885d54c-9fb2-4f01-8937-54f8ac1ce5bc"

    init {
        test("Arahbo's Cat token carries the Foundations Cat art, not the generic one") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Arahbo, the First Fang")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .build()

            game.castSpell(1, "Arahbo, the First Fang").error shouldBe null
            game.resolveStack()

            val catId = game.findPermanent("Cat Token")
            catId shouldNotBe null
            val art = game.state.getEntity(catId!!)?.get<CardComponent>()?.imageUri

            art shouldNotBe null
            art!! shouldContain foundationsCat
            // Specifically *not* the set-agnostic fallback, which is what every Cat-making card
            // outside Foundations still gets.
            art shouldNotBe TokenArt.IMAGES["Cat"]
        }

        test("Mirrodin's Soldier now carries Mirrodin's own art") {
            // Raise the Alarm is canonically Mirrodin, one of the ~57 sets that predate token
            // cards: no `tmrd` on Scryfall to sync, so its Soldier rendered with the engine-wide
            // generic art until MRD gained a self-hosted `tokenArt` row.
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Raise the Alarm")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Plains")
                .build()

            game.castSpell(1, "Raise the Alarm").error shouldBe null
            game.resolveStack()

            val soldier = game.findPermanent("Soldier Token")
            soldier shouldNotBe null
            val art = game.state.getEntity(soldier!!)?.get<CardComponent>()?.imageUri
            art shouldBe "/images/tokens/mrd-soldier.jpeg"
            art shouldNotBe TokenArt.IMAGES["Soldier"]
        }

        test("a set that prints no matching token still falls back to the generic table") {
            // The floor the whole scheme rests on: a set with token art of its own is not thereby
            // claiming to print *every* token its cards mint. Foundations prints a Cat and (by way
            // of the Release the Dogs reprint) Dogs; its Bird has no row, so that one must still
            // reach the engine-wide generic table rather than resolving to a sibling's art.
            val registry = TokenArtRegistry().apply {
                register(
                    FoundationsSet.code,
                    TokenArtData.forSet(FoundationsSet),
                    FoundationsSet.cards.map { it.name },
                )
            }

            registry.resolveAll(
                sourceCardDefinitionId = "Arahbo, the First Fang#FDN-2",
                tokenName = "Cat",
            ).shouldNotBeEmpty()
            registry.resolve(
                sourceCardDefinitionId = "Arahbo, the First Fang#FDN-2",
                tokenName = "Griffin",
            ) shouldBe null
        }

        test("Release the Dogs deals out all four of Jumpstart's Dog arts") {
            // Jumpstart printed the Dog token four times with four illustrations, and the card
            // makes exactly four — so the board should show four different dogs, not one dog four
            // times. This is what TokenPrinting's several-rows-per-token shape buys.
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Release the Dogs")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Plains")
                .build()

            game.castSpell(1, "Release the Dogs").error shouldBe null
            game.resolveStack()

            val dogs = game.findPermanents("Dog Token")
            dogs shouldHaveSize 4
            dogs.map { game.state.getEntity(it)?.get<CardComponent>()?.imageUri } shouldBe listOf(
                "/images/tokens/jmp-dog1.jpeg",
                "/images/tokens/jmp-dog2.jpeg",
                "/images/tokens/jmp-dog3.jpeg",
                "/images/tokens/jmp-dog4.jpeg",
            )
        }

        test("the Foundations reprint deals out the same four Dog arts") {
            // A reprint normally mints its *own* set's token, and `tfdn` prints a single Dog — which
            // would put the same dog on the battlefield four times. FDN therefore borrows the four
            // Jumpstart arts explicitly. Asserted through the printing-qualified id a real game
            // carries ("Name#SET-CN"), which is the path that routes a reprint to its own set;
            // the scenario builder keys entities by bare name and so always lands on JMP.
            val registry = TokenArtRegistry().apply {
                for (set in listOf(FoundationsSet, JumpstartSet)) {
                    register(set.code, TokenArtData.forSet(set), set.cards.map { it.name })
                }
            }

            registry.resolveAll(
                sourceCardDefinitionId = "Release the Dogs#FDN-580",
                tokenName = "Dog",
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
            ) shouldBe listOf(
                "/images/tokens/jmp-dog1.jpeg",
                "/images/tokens/jmp-dog2.jpeg",
                "/images/tokens/jmp-dog3.jpeg",
                "/images/tokens/jmp-dog4.jpeg",
            )
        }
    }
}

