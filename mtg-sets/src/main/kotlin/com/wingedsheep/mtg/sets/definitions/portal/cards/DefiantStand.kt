package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep

/**
 * Defiant Stand
 * {1}{W}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Target creature gets +1/+3 until end of turn. Untap that creature.
 */
val DefiantStand = card("Defiant Stand") {
    manaCost = "{1}{W}"
    typeLine = "Instant"

    spell {
        // Cast restrictions
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)

        val t = target("target", Targets.Creature)
        effect = Effects.Composite(
            Effects.ModifyStats(1, 3, t),
            Effects.Untap(t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Hannibal King"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cc37fd2-5c34-4522-8113-e6dd2181550b.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
