package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serendib Djinn
 * {2}{U}{U}
 * Creature — Djinn
 * 5/6
 * Flying
 * At the beginning of your upkeep, sacrifice a land. If you sacrifice an Island this way,
 * this creature deals 3 damage to you.
 * When you control no lands, sacrifice this creature.
 *
 * Composition (no new engine features needed):
 *  - Upkeep trigger fires `Effects.Sacrifice(Land, target = Controller)` so you choose which
 *    land to sacrifice during resolution (CR ruling: the land is chosen on resolution). The
 *    sacrificed permanent's snapshot flows into `EffectContext.sacrificedPermanents`.
 *  - The self-damage rider is a `ConditionalEffect` gated on `SacrificedHadSubtype("Island")`,
 *    reading that snapshot — same pattern as Rise of the Witch-king's "if you sacrificed a
 *    creature this way" rider. `damageSource = Self` so the 3 damage comes from this creature.
 *  - "When you control no lands, sacrifice this creature" is a `stateTriggeredAbility`
 *    (CR 603.8 state trigger), mirroring Dandân's "when you control no Islands" sacrifice.
 */
val SerendibDjinn = card("Serendib Djinn") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn"
    power = 5
    toughness = 6
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, sacrifice a land. If you sacrifice an Island this way, " +
        "this creature deals 3 damage to you.\n" +
        "When you control no lands, sacrifice this creature."
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.Sacrifice(
            GameObjectFilter.Land,
            count = 1,
            target = EffectTarget.Controller
        ).then(
            ConditionalEffect(
                condition = Conditions.SacrificedHadSubtype("Island"),
                effect = Effects.DealDamage(
                    3,
                    EffectTarget.Controller,
                    damageSource = EffectTarget.Self
                )
            )
        )
        description = "At the beginning of your upkeep, sacrifice a land. " +
            "If you sacrifice an Island this way, this creature deals 3 damage to you."
    }

    stateTriggeredAbility {
        condition = Conditions.YouControl(GameObjectFilter.Land, negate = true)
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control no lands, sacrifice this creature"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0458b733-d689-4cb5-8970-3b675c67fc4d.jpg?1562895874"
    }
}
