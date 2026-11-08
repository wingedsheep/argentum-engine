package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Itzquinth, Firstborn of Gishath
 * {R}{G}
 * Legendary Creature — Dinosaur
 * 2/3
 * Haste
 * When Itzquinth enters, you may pay {2}. When you do, target Dinosaur you control deals
 * damage equal to its power to another target creature.
 *
 * The "When you do" phrasing is a reflexive trigger (CR 603.12): the optional {2} payment
 * ([Effects.MayPay]) gates an [Effects.ReflexiveTrigger]. The ETB trigger resolves to the "Pay {2}?" decision;
 * only after payment does the reflexive trigger go on the stack and prompt for its two targets:
 *   - t1 (index 0): target Dinosaur you control — any Creature with subtype Dinosaur you
 *     control (including Itzquinth itself, which IS a Dinosaur).
 *   - t2 (index 1): another target creature — [TargetOther] ensures this differs from t1.
 * Damage amount = t1's power at resolution ([DynamicAmounts.targetPower(0)]), and the source
 * of the damage is t1 ([damageSource = t1]), so effects that care about the dealer reference
 * the Dinosaur, not Itzquinth.
 */
val ItzquinthFirstbornOfGishath = card("Itzquinth, Firstborn of Gishath") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Dinosaur"
    power = 2
    toughness = 3
    oracleText = "Haste\nWhen Itzquinth enters, you may pay {2}. When you do, target Dinosaur you control deals damage equal to its power to another target creature."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        // "you may pay {2}. When you do" → the optional payment gates a reflexive trigger.
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "target Dinosaur you control deals " +
                    "damage equal to its power to another target creature.",
            ) {
                // t1 (index 0): the Dinosaur you control that deals the damage.
                val t1 = target(TargetFilter.Creature.withSubtype("Dinosaur").youControl())
                // t2 (index 1): must be a different creature from t1 (the "another" constraint).
                val t2 = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
                effect = Effects.DealDamage(DynamicAmounts.powerOf(t1), t2, damageSource = t1)
            },
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Lars Grant-West"
        flavorText = "\"Dinosaurs have no concept of royalty, but they recognize the scent of the mightiest among them.\"\n—Atla Palani, nest tender"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7112c366-b36a-4bc8-aa64-6bad16bebc39.jpg?1782694426"
    }
}
