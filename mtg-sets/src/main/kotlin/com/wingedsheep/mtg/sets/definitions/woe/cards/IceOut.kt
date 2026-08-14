package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Ice Out
 * {1}{U}{U}
 * Instant
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * This spell costs {1} less to cast if it's bargained.
 * Counter target spell.
 *
 * The **cost-gate** shape of bargain (CR 702.166), same as [JohannsStopgap] and [HamletGlutton]: a
 * `SelfCast` [ModifySpellCost] with `ReduceGeneric(1)` gated by
 * `CostGating.OnlyIf(`[Conditions.WasBargained]`)`. The bargained cast prices at {U}{U}; the plain
 * cast still costs {1}{U}{U}. The reduction touches only the total cost paid — Ice Out's mana value
 * stays 3 for anything that reads it.
 *
 * The counter half carries no bargain gate: bargain buys a discount here, not extra effect, so the
 * spell resolves identically either way.
 */
val IceOut = card("Ice Out") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "This spell costs {1} less to cast if it's bargained.\n" +
        "Counter target spell."

    bargain()

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.OnlyIf(Conditions.WasBargained),
        )
    }

    spell {
        target = Targets.Spell
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Olivier Bernard"
        flavorText = "Hylda built an ice statuary from all those who couldn't understand \"go away.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88ffafda-c852-497a-8156-4f759cdf3693.jpg?1783915120"

        ruling(
            "2023-09-01",
            "Bargain represents an optional additional cost. A spell cast with that additional " +
                "cost paid is \"bargained.\""
        )
        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "If you copy a bargained spell, the copy is also bargained."
        )
    }
}
