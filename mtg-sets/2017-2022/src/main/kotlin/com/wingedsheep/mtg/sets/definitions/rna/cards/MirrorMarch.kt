package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mirror March
 * {5}{R}
 * Enchantment
 * Whenever a nontoken creature you control enters, flip a coin until you lose a flip. For each flip
 * you won, create a token that's a copy of that creature. Those tokens gain haste. Exile them at the
 * beginning of the next end step.
 */
val MirrorMarch = card("Mirror March") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control enters, flip a coin until you lose a flip. " +
        "For each flip you won, create a token that's a copy of that creature. Those tokens gain haste. " +
        "Exile them at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).enters()
        effect = Effects.FlipCoinsUntilLoss(storeWinsAs = "mirrorMarchWins", mayStop = false) then
            Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.TriggeringEntity,
                count = DynamicAmounts.storedNumber("mirrorMarchWins"),
                addedKeywords = setOf(Keyword.HASTE),
                exileAtStep = Step.END,
            )
        description = "Whenever a nontoken creature you control enters, flip a coin until you lose a flip. " +
            "For each flip you won, create a token that's a copy of that creature. Those tokens gain haste. " +
            "Exile them at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50a27943-f08c-4cbd-affb-8e59411b3d4f.jpg?1783933678"
        ruling("2019-01-25", "All of the tokens enter the battlefield at the same time.")
        ruling(
            "2019-01-25",
            "If the entering creature leaves the battlefield before Mirror March's ability resolves, use that " +
                "creature's last known existence on the battlefield to determine the token's characteristics."
        )
        ruling(
            "2019-01-25",
            "Any enters-the-battlefield abilities of the copied creature will trigger when the token enters the battlefield."
        )
    }
}
