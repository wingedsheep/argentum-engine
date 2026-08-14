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
 * Johann's Stopgap
 * {3}{U}
 * Sorcery
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * This spell costs {2} less to cast if it's bargained.
 * Return target nonland permanent to its owner's hand. Draw a card.
 *
 * The **cost-gate** shape of bargain (CR 702.166), same as [HamletGlutton]: a `SelfCast`
 * [ModifySpellCost] with `ReduceGeneric(2)` gated by `CostGating.OnlyIf(`[Conditions.WasBargained]`)`,
 * so the reduction prices the bargained cast branch and the plain cast still costs {3}{U}. Per the
 * ruling below the reduction touches only the total cost paid — the Stopgap's mana value stays 4,
 * which matters for anything that reads it (Chalice of the Void, Vantress Transmuter, …).
 *
 * The spell half needs no bargain gate at all: bouncing and drawing happen on every cast. Both
 * clauses share the one target requirement, so per the ruling an illegal target by resolution
 * fizzles the *whole* spell — no card is drawn.
 */
val JohannsStopgap = card("Johann's Stopgap") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "This spell costs {2} less to cast if it's bargained.\n" +
        "Return target nonland permanent to its owner's hand. Draw a card."

    bargain()

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.WasBargained),
        )
    }

    spell {
        val permanent = target("target nonland permanent", Targets.NonlandPermanent)
        effect = Effects.Composite(
            Effects.ReturnToHand(permanent),
            Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Christina Kraus"
        flavorText = "\"Just ... stay in there ... for one ... second!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31408397-36f5-479f-b822-fa97411b7872.jpg?1783915118"

        ruling(
            "2023-09-01",
            "The cost-reduction ability of Johann's Stopgap doesn't change its mana cost or mana " +
                "value, only the total cost you pay. Specifically, the mana value of Johann's " +
                "Stopgap is always 4."
        )
        ruling(
            "2023-09-01",
            "If the permanent is an illegal target as Johann's Stopgap tries to resolve, Johann's " +
                "Stopgap is removed from the stack and you won't draw a card."
        )
        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
    }
}
