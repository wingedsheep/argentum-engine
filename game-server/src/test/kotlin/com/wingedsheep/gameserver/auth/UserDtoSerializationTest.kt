package com.wingedsheep.gameserver.auth

import com.wingedsheep.gameserver.controller.AdminUsersController
import com.wingedsheep.gameserver.controller.AuthController
import com.wingedsheep.gameserver.stats.AdminUserStat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Pins the wire shape of the `isAdmin` flag on every account DTO the client deserializes.
 *
 * There is no jackson-module-kotlin on the REST classpath, so Jackson serializes via the JavaBean
 * `isAdmin()` getter and — left unannotated — emits the key `admin`. The client reads `user.isAdmin`
 * / `u.isAdmin` / `detail.isAdmin` (the Admin button, the players-list badge, the promote toggle), so
 * the dropped `is` prefix made a promoted account look un-promoted everywhere. `@JsonProperty` pins
 * the name. The mapper is taken straight off Spring MVC's own JSON converter, so the test serializes
 * a response exactly the way the server does.
 */
class UserDtoSerializationTest : FunSpec({
    val mapper: ObjectMapper = JacksonJsonHttpMessageConverter().mapper
    val id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    test("AuthController.UserDto serializes the admin flag as isAdmin") {
        val dto = AuthController.UserDto(id, "a@b.com", "A", isAdmin = true, hidePresence = false)
        mapper.writeValueAsString(dto) shouldContain "\"isAdmin\":true"
    }

    test("AdminUsersController.UserDetailDto serializes the admin flag as isAdmin") {
        val dto = AdminUsersController.UserDetailDto(
            id = id, email = "a@b.com", displayName = "A", isAdmin = true, createdAt = "now",
            stats = AdminUsersController.StatsDto(0, 0, 0, 0.0),
            colors = emptyList(), modes = emptyList(), opponents = emptyList(),
            topCards = emptyList(), tournaments = emptyList(), recentGames = emptyList(),
        )
        mapper.writeValueAsString(dto) shouldContain "\"isAdmin\":true"
    }

    test("AdminUserStat serializes the admin flag as isAdmin") {
        val dto = AdminUserStat(
            id = id, email = "a@b.com", displayName = "A", isAdmin = true, createdAt = "now",
            games = 0, wins = 0, lastPlayed = null,
        )
        mapper.writeValueAsString(dto) shouldContain "\"isAdmin\":true"
    }
})
