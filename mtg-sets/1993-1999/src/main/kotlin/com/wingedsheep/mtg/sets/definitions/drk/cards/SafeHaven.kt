package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Safe Haven
 * Land
 * {2}, {T}: Exile target creature you control.
 * At the beginning of your upkeep, you may sacrifice this land. If you do, return each card exiled
 * with this land to the battlefield under its owner's control.
 *
 * "Each card exiled **with this land**" is a linked-exile pile, not a general "return everything
 * from exile": `ExileLinkedToSource` records each creature against the Haven, and
 * `ReturnLinkedExileUnderOwnersControl` gives exactly that pile back — a second Safe Haven never
 * returns the first one's creatures.
 *
 * "Under its owner's control", not yours, so a creature you took control of and then exiled here
 * comes back to its owner. That is the reason for the `UnderOwnersControl` variant rather than the
 * plain return.
 *
 * The sacrifice is inside the trigger rather than a cost of it — the printed text is "you may
 * sacrifice … If you do", so an `IfYouDo` gating the return on the sacrifice actually happening.
 * Since the land is gone by then, a Haven destroyed in response to its own trigger takes its pile
 * with it, exactly as the card does.
 */
val SafeHaven = card("Safe Haven") {
    typeLine = "Land"
    oracleText = "{2}, {T}: Exile target creature you control.\n" +
        "At the beginning of your upkeep, you may sacrifice this land. If you do, return each " +
        "card exiled with this land to the battlefield under its owner's control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        target = Targets.CreatureYouControl
        effect = Effects.ExileLinkedToSource(EffectTarget.ContextTarget(0))
        description = "{2}, {T}: Exile target creature you control."
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        optional = true
        effect = Effects.IfYouDo(
            action = Effects.SacrificeTarget(EffectTarget.Self),
            ifYouDo = Effects.ReturnLinkedExileUnderOwnersControl(),
            // "If you do" gates on the sacrifice actually happening. A sacrifice is a zone move,
            // but Auto can't infer it from the action's shape, and Always would fail open — a
            // Safe Haven that has already left the battlefield would still return its exiles.
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "At the beginning of your upkeep, you may sacrifice this land. If you do, " +
            "return each card exiled with this land to the battlefield under its owner's control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d48fb47-1bed-4791-a014-504515f3d36f.jpg?1783947922"
    }
}
