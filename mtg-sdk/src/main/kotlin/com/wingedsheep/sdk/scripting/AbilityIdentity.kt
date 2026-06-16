package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Stable identity of a *kind* of ability, independent of the specific stack object or source
 * entity instance.
 *
 * Two permanents printed from the same card definition (e.g. two Soul Wardens) — and every future
 * instance of that card — share one [AbilityIdentity] for a given ability, because both halves are
 * definition-scoped:
 *  - [cardDefinitionId] is the source card's definition id
 *    (`CardComponent.cardDefinitionId`, e.g. `"Soul Warden#ALA-25"`), shared by every entity
 *    created from the same [com.wingedsheep.sdk.model.CardDefinition].
 *  - [abilityId] is the ability's [AbilityId], generated once when the card definition is built and
 *    therefore identical across every entity created from that definition.
 *
 * This is the single shared key used to
 *  - **group** structurally identical triggers so a repeated "you may …" decision can be answered
 *    once (batch decisions), and
 *  - **remember** a player's persistent yield / auto-answer preference for an ability across all of
 *    its copies and future instances (persistent yields).
 *
 * It is deliberately NOT keyed by entity id (which differs per instance) nor by rendered text
 * (which can collide between genuinely different abilities). See
 * `backlog/stack-collapse-and-batch-decisions.md` §C.2.
 */
@Serializable
data class AbilityIdentity(
    val cardDefinitionId: String,
    val abilityId: AbilityId,
)
