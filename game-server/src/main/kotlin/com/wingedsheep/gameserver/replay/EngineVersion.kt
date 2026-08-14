package com.wingedsheep.gameserver.replay

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * The build identity stamped onto every recorded replay.
 *
 * We can't run old code, so this never changes how a replay is reconstructed — it changes how fast
 * you can explain one. "This replay diverges" is a mystery; "this replay was recorded on
 * `a3f91c2`, eleven deploys ago, and the divergence is at the first Bloodghast trigger" is a bug
 * report. Set `APP_VERSION` at build time (the deploy pipeline passes the git sha, the same one the
 * web client already gets); defaults to `dev` locally.
 */
@Component
class EngineVersion(
    @Value("\${app.version:dev}") val value: String,
)
