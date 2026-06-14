package com.wingedsheep.engine.state.components.stack

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AbilityIdentity

/**
 * Derives the [AbilityIdentity] of an ability whose source permanent/card is [sourceId], by pairing
 * the source's `CardComponent.cardDefinitionId` with the ability's [abilityId].
 *
 * Returns `null` when [sourceId] has no [CardComponent] — e.g. synthesized sources such as a spell
 * copy placed on a fresh entity — in which case the ability simply carries no identity and is never
 * grouped or yielded against (correct: a copy of a spell is not a recurring card ability).
 *
 * This is the one place the key is computed, so triggered- and activated-ability stack components
 * and the decisions they raise all agree on the same identity.
 */
fun GameState.abilityIdentityOf(sourceId: EntityId, abilityId: AbilityId): AbilityIdentity? =
    getEntity(sourceId)?.get<CardComponent>()?.cardDefinitionId
        ?.let { AbilityIdentity(it, abilityId) }
