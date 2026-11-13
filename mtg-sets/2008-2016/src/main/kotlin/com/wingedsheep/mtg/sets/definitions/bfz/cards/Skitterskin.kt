package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skitterskin — Battle for Zendikar #97
 * {3}{B} · Creature — Eldrazi Drone · 4/3
 * Devoid (This card has no color.)
 * This creature can't block.
 * {1}{B}: Regenerate this creature. Activate only if you control another colorless creature.
 *
 * "Another colorless creature" is Eldrazi Aggressor's `YouControl(… IsColorless, excludeSelf = true)`,
 * gated here as an activation restriction (Spire of Industry's `OnlyIfCondition` shape). It is checked
 * only on activation, so the shield still forms if that creature leaves in response (2015-08-25 ruling).
 */
val Skitterskin = card("Skitterskin") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 4
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "This creature can't block.\n" +
        "{1}{B}: Regenerate this creature. Activate only if you control another colorless creature."

    keywords(Keyword.DEVOID)

    staticAbility {
        ability = CantBlock()
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(
                    GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless),
                    excludeSelf = true,
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3cbecdc-bc30-4797-bf6f-a7a61db05b48.jpg?1783938204"
        ruling(
            "2015-08-25",
            "Once you legally activate the ability, it doesn't matter what happens to the other colorless " +
                "creature. The ability will resolve and create a regeneration shield even if you don't control " +
                "another colorless creature at that time."
        )
        ruling(
            "2015-08-25",
            "Skitterskin's activated ability doesn't depend on Skitterskin being colorless. If it gains one or " +
                "more colors, you can still activate the ability if you control another creature that's colorless."
        )
    }
}
