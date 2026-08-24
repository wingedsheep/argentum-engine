package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.effects.GainAllActivatedAbilitiesOfEffect
import kotlin.reflect.KClass

/**
 * Executor for [GainAllActivatedAbilitiesOfEffect].
 * "This creature gains all activated abilities of target creature until end of turn"
 * (Quicksilver Elemental).
 *
 * Reads the donor's printed activated abilities **once, here**, and writes one
 * [GrantedActivatedAbility] per ability onto the receiver. Snapshotting at resolution is the
 * printed behaviour, per the Havengul Lich ruling ("gains the activated abilities of the card as it
 * existed in the graveyard"): the donor leaving, changing, or gaining abilities afterwards doesn't
 * change what the receiver has. That is what separates this from the continuously re-read static
 * [com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents] (Sharkey, Marvin).
 *
 * `GameState.grantedActivatedAbilities` is the right store rather than a granted
 * `GrantActivatedAbility` static, because it is the one every read site already consults: the
 * activated-ability enumerator, `ActivateAbilityHandler`, `LandManaColorInspector` (so a gained
 * mana ability really produces mana), and `CleanupPhaseManager` / `EndedDurationExpiryCheck` for
 * the duration.
 *
 * Two details that are load-bearing:
 *  - The grant is keyed to the **receiver**, so a copied ability's `{T}`, `SacrificeSelf` and
 *    "this creature" bind to the permanent that gained it (CR 113.7) — the printed reminder text
 *    "(If any of the abilities use that creature's name, use this creature's name instead.)".
 *  - Each gained ability is re-stamped with a donor-derived [AbilityId]. Two donors sharing a card
 *    definition otherwise carry the *same* printed id, and the enumerator's `distinctBy { id }`
 *    would collapse them into one; the re-stamp also gives each its own once-per-turn budget, which
 *    is the printed ruling ("If you make two copies of an ability that can be activated once a
 *    turn, you can activate each of them once a turn"). Same technique, same reason, as
 *    `CastPermissionUtils.donorCardsActivatedAbilities`'s `oncePerTurnEach` re-stamp.
 *
 * Only *activated* abilities activatable from the battlefield are copied — never triggered, static,
 * or keyword abilities — matching `getGainedAbilitiesOfPermanents`. Mana abilities are included:
 * the printed wording is "all activated abilities", with no "except mana abilities" clause.
 */
class GainAllActivatedAbilitiesOfExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<GainAllActivatedAbilitiesOfEffect> {

    override val effectType: KClass<GainAllActivatedAbilitiesOfEffect> =
        GainAllActivatedAbilitiesOfEffect::class

    override fun execute(
        state: GameState,
        effect: GainAllActivatedAbilitiesOfEffect,
        context: EffectContext
    ): EffectResult {
        val receiverId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid recipient for the ability gain")
        if (!state.getBattlefield().contains(receiverId)) {
            return EffectResult.error(state, "The recipient is not on the battlefield")
        }

        // The donor may have left the battlefield between announcement and resolution. That is not
        // an error — the ability just gains nothing (CR 608.2b: an effect does as much as it can).
        val donorId = context.resolveTarget(effect.donor)
        val donorEntity = donorId?.let { state.getEntity(it) }
        val donorCard = donorEntity?.get<CardComponent>()
        val donorDef = donorCard?.let { cardRegistry.getCard(it.cardDefinitionId) }
            ?: return EffectResult.success(state)
        val donorClassLevel = donorEntity.get<ClassLevelComponent>()?.currentLevel

        val gained = donorDef.script.effectiveActivatedAbilities(donorClassLevel)
            .filter { it.activateFromZone == Zone.BATTLEFIELD }
            .map { ability ->
                GrantedActivatedAbility(
                    entityId = receiverId,
                    ability = ability.copy(
                        id = AbilityId("gained_${donorId.value}_${ability.id.value}")
                    ),
                    duration = effect.duration,
                    sourceId = context.sourceId
                )
            }
        if (gained.isEmpty()) return EffectResult.success(state)

        return EffectResult.success(
            state.copy(grantedActivatedAbilities = state.grantedActivatedAbilities + gained)
        )
    }
}
