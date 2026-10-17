package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Chill of the Grave
 * {2}{U}
 * Instant
 *
 * This spell costs {1} less to cast if you control a Zombie.
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Draw a card.
 *
 * The discount is a flat {1}, not {1} per Zombie — an ordinary self-cast [ModifySpellCost] gated by
 * [CostGating.OnlyIf], evaluated as the spell is cast (CR 601.2f).
 */
val ChillOfTheGrave = card("Chill of the Grave") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if you control a Zombie.\n" +
        "Tap target creature. It doesn't untap during its controller's next untap step.\n" +
        "Draw a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.OnlyIf(
                Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE))
            )
        )
    }

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            ) then
            Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Olivier Bernard"
        flavorText = "Frozen is frozen, whether it's with cold, terror, or both."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60222e91-a688-4113-a8c2-ab08f52bb6e1.jpg?1783924898"
    }
}
