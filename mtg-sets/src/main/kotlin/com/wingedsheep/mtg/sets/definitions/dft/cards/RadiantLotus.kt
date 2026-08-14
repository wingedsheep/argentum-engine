package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Radiant Lotus — {6}
 * Artifact
 * Mythic Rare — Aetherdrift #240
 *
 * "{T}, Sacrifice one or more artifacts: Choose a color. Target player adds three mana of the
 *  chosen color for each artifact sacrificed this way."
 *
 * A Black Lotus that pays someone else. Two SDK axes carry it:
 *
 *  - [Costs.SacrificePermanents] — the sacrificing twin of `Costs.ExilePermanents`: a
 *    *variable-count* cost where the payer chooses how many artifacts to sacrifice (at least one),
 *    and that count becomes the ability's X (CR 601.2b — a variable defined by a cost choice is
 *    announced as the ability is activated). `excludeSelf = false` because Radiant Lotus is itself
 *    an artifact and may be one of the artifacts sacrificed to its own cost; tapping it and
 *    sacrificing it are both legal parts of the same cost (CR 601.2h, costs paid in any order).
 *  - [Effects.AddManaOfChoice]'s `recipient` — "**target player** adds …", so the mana lands in the
 *    target's pool rather than the controller's. The *colour* is still the controller's choice:
 *    "Choose a color" names no other chooser.
 *
 * Because it targets, this is **not** a mana ability (CR 605.1a) — it uses the stack, can be
 * responded to, and can fizzle if the target becomes illegal. The mana therefore arrives during the
 * target player's own priority window and, like all mana, empties at the end of the step or phase
 * (CR 500.4) — so it's the target player who has to spend it.
 *
 * Three mana per artifact is `DynamicAmount.Multiply(XValue, 3)`, evaluated at resolution against
 * the X stored on the stack, so removing artifacts in response can't change the amount.
 */
val RadiantLotus = card("Radiant Lotus") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice one or more artifacts: Choose a color. Target player adds three " +
        "mana of the chosen color for each artifact sacrificed this way."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificePermanents(GameObjectFilter.Artifact, minCount = 1, excludeSelf = false)
        )
        val player = target("target player", TargetPlayer())
        effect = Effects.AddManaOfChoice(
            colorSet = ManaColorSet.AnyColor,
            amount = DynamicAmount.Multiply(DynamicAmount.XValue, 3),
            recipient = player,
        )
        description = "{T}, Sacrifice one or more artifacts: Choose a color. Target player adds " +
            "three mana of the chosen color for each artifact sacrificed this way."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "240"
        artist = "Bruce Brenneise"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be6dac83-39c2-40dc-a322-76a3ea4e7aee.jpg?1783907846"
    }
}
