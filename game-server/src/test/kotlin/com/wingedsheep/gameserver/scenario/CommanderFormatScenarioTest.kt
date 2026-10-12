package com.wingedsheep.gameserver.scenario

import com.wingedsheep.engine.state.components.player.CompanionComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A dev scenario can be a Commander game ([ScenarioRequest.commanderFormat]): the engine runs under
 * `Format.Commander`, seats start at its 40 life unless they set their own, and a seat can hold a
 * commander and a companion together.
 */
class CommanderFormatScenarioTest : ScenarioTestBase() {

    private val service get() = ScenarioBuilderService(cardRegistry)

    init {
        test("commanderFormat runs Commander at 40 life, with a commander and a companion side by side") {
            val build = service.buildScenario(
                ScenarioRequest(
                    player1 = PlayerConfig(
                        commanders = listOf("Sovereign Okinec Ahau"),
                        companion = "Kaheera, the Orphanguard",
                    ),
                    player2 = PlayerConfig(lifeTotal = 25),
                    commanderFormat = true,
                )
            )
            val s = build.state
            val alice = EntityId.of("player-1")

            s.format.shouldBeInstanceOf<Format.Commander>()
            s.lifeTotal(alice) shouldBe 40
            s.lifeTotal(EntityId.of("player-2")) shouldBe 25
            s.getEntity(alice)?.get<CommanderRegistryComponent>()?.commanderIds.shouldNotBeNull().shouldHaveSize(1)
            s.getEntity(alice)?.get<CompanionComponent>().shouldNotBeNull()
        }

        test("without commanderFormat the scenario keeps the default format and 20 life") {
            val s = service.buildScenario(ScenarioRequest()).state
            (s.format is Format.Commander) shouldBe false
            s.lifeTotal(EntityId.of("player-1")) shouldBe 20
        }
    }
}
