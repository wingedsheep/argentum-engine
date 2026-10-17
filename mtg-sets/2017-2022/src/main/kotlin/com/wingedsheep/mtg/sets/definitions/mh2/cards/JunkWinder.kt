package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Junk Winder
 * {5}{U}{U}
 * Creature — Serpent
 * 5/6
 * Affinity for tokens (This spell costs {1} less to cast for each token you control.)
 * Whenever a token you control enters, tap target nonland permanent an opponent controls. It
 * doesn't untap during its controller's next untap step.
 *
 * "Affinity for tokens" has no type/subtype to hang `KeywordAbility.Affinity` on, so it is the
 * self-cast cost reduction it abbreviates (the Qumulox shape) over `GameObjectFilter.Any.token()` —
 * any token counts, not just creature tokens. The trigger is per token (Wildwood Mentor's shape);
 * the freeze is Entangling Trap's `DOESNT_UNTAP` for the affected controller's next untap step.
 */
val JunkWinder = card("Junk Winder") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 5
    toughness = 6
    oracleText = "Affinity for tokens (This spell costs {1} less to cast for each token you control.)\n" +
        "Whenever a token you control enters, tap target nonland permanent an opponent controls. " +
        "It doesn't untap during its controller's next untap step."

    // Affinity for tokens
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(GameObjectFilter.Any.token())
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl().token()).enters()
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.Tap(permanent) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                permanent,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        description = "Whenever a token you control enters, tap target nonland permanent an " +
            "opponent controls. It doesn't untap during its controller's next untap step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66b7833a-a6b6-4aca-b574-66a6aea471ea.jpg?1783926877"
    }
}
