package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * The printed static abilities live on the permanent [entityId]: the card's base statics plus, for a
 * Class, those of every level it has reached (CR 716.2a). A scan that reads `script.staticAbilities`
 * directly sees only the base statics, so a level-gated permission (Fortune Teller's Talent's level 2
 * "play cards from the top of your library") silently never applies.
 */
fun CardDefinition.liveStaticAbilities(state: GameState, entityId: EntityId): List<StaticAbility> =
    script.effectiveStaticAbilities(state.getEntity(entityId)?.get<ClassLevelComponent>()?.currentLevel)
