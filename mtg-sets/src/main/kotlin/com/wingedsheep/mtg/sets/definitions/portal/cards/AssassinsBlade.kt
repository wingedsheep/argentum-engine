package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.YouWereAttackedThisStep
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Assassin's Blade
 * {1}{B}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Destroy target nonblack attacking creature.
 */
val AssassinsBlade = card("Assassin's Blade") {
    manaCost = "{1}{B}"
    typeLine = "Instant"

    spell {
        // Cast restrictions
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)

        target = TargetCreature(filter = TargetFilter.AttackingCreature.notColor(Color.BLACK))
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b80e8fe0-eccb-4268-a6ce-1365c68e6b13.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
