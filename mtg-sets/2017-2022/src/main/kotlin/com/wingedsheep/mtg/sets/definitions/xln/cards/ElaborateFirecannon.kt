package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elaborate Firecannon
 * {2}
 * Artifact
 * This artifact doesn't untap during your untap step.
 * {4}, {T}: This artifact deals 2 damage to any target.
 * At the beginning of your upkeep, you may discard a card. If you do, untap this artifact.
 *
 * The untap clause is the unconditional [AbilityFlag.DOESNT_UNTAP] self-suppression (Leviathan's
 * shape); the upkeep trigger is Immersturm Raider's [Effects.May] around [Effects.IfYouDo] with a
 * one-card discard as the action, so declining or holding an empty hand leaves the cannon tapped.
 */
val ElaborateFirecannon = card("Elaborate Firecannon") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "This artifact doesn't untap during your untap step.\n" +
        "{4}, {T}: This artifact deals 2 damage to any target.\n" +
        "At the beginning of your upkeep, you may discard a card. If you do, untap this artifact."

    flags(AbilityFlag.DOESNT_UNTAP)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.Untap(EffectTarget.Self))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Adrian Majkrzak"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b9170f0-d332-42ba-98c7-7e99922fa3fb.jpg?1783935704"
    }
}
