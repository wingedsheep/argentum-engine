package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Frontier Mastodon — {2}{G} 3/2 that enters with a +1/+1 counter if you control a creature with
 * power 4 or greater. The ferocious check happens as it enters, so (per the 2014-11-24 ruling) it
 * never counts itself, even when an anthem would make it 4 power; and only *your* creatures count.
 */
class FrontierMastodonScenarioTest : ScenarioTestBase() {

    init {
        context("Frontier Mastodon") {

            fun plusOneCounters(game: TestGame): Int =
                game.findPermanent("Frontier Mastodon")?.let { id ->
                    game.state.getEntity(id)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE)
                } ?: 0

            fun castMastodon(game: TestGame) {
                val cast = game.castSpell(1, "Frontier Mastodon")
                withClue("the cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
            }

            test("enters with a counter when you control a creature with power 4 or greater") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Frontier Mastodon")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .build()

                castMastodon(game)
                plusOneCounters(game) shouldBe 1
            }

            test("enters with no counter when only an opponent controls a 4-power creature") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Frontier Mastodon")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .build()

                castMastodon(game)
                plusOneCounters(game) shouldBe 0
            }

            test("does not count itself even when an anthem makes it 4 power") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Frontier Mastodon")
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .build()

                castMastodon(game)
                withClue("the Mastodon isn't on the battlefield as the check happens") {
                    plusOneCounters(game) shouldBe 0
                }
            }
        }
    }
}
