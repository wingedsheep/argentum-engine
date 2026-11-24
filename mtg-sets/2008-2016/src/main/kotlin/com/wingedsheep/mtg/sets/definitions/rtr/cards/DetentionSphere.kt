package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Detention Sphere
 * {1}{W}{U}
 * Enchantment
 * When this enchantment enters, you may exile target nonland permanent not named Detention Sphere
 * and all other permanents with the same name as that permanent.
 * When this enchantment leaves the battlefield, return the exiled cards to the battlefield under
 * their owner's control.
 *
 * Oblivion Ring's two-trigger shape crossed with Deputy of Detention's same-name sweep. The exile is
 * [Effects.ExileLinkedToSource] — not a duration-scoped exile, and not `ExileUntilLeaves` (which does
 * nothing once the source is gone) — so if the Sphere leaves before its enters trigger resolves, the
 * leaves trigger finds nothing and the cards stay exiled indefinitely (2012-10-01 ruling); the link
 * lands on the departed visit and is never returned. The target is exiled through its own handle and
 * the other same-named permanents — any controller's, lands included — are gathered with
 * `excludeChosenTargets`, so untargeted ones go even with protection. A face-down target has no name,
 * so it is exiled alone.
 */
val DetentionSphere = card("Detention Sphere") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you may exile target nonland permanent not named Detention " +
        "Sphere and all other permanents with the same name as that permanent.\n" +
        "When this enchantment leaves the battlefield, return the exiled cards to the battlefield under " +
        "their owner's control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val t = target(TargetFilter(GameObjectFilter.NonlandPermanent.notNamed("Detention Sphere")))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val targetName = storeCardName(filter(chosen, GameObjectFilter.Any.faceUp()))
            val sameNamed = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Permanent.namedFromVariable(targetName),
                    excludeChosenTargets = true,
                )
            )
            run(Effects.ExileLinkedToSource(t))
            run(
                Effects.ForEachInCollection(
                    sameNamed,
                    Effects.ExileLinkedToSource(EffectTarget.IterationEntity)
                )
            )
        }
        description = "When this enchantment enters, you may exile target nonland permanent not named " +
            "Detention Sphere and all other permanents with the same name as that permanent."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/afee5464-83b7-4d7a-b407-9ee7de21535b.jpg?1783940341"

        ruling(
            "2012-10-01",
            "Although the target of the \"enters\" ability must not be a land, lands with the same name as that " +
                "permanent will be exiled."
        )
        ruling(
            "2012-10-01",
            "The \"enters\" ability has only one target. The other permanents with that name aren't targeted. " +
                "For example, a permanent with protection from white will be exiled if it has the same name as " +
                "the target nonland permanent."
        )
        ruling(
            "2012-10-01",
            "If the target nonland permanent is an illegal target when the \"enters\" ability tries to resolve, " +
                "it won't resolve and none of its effects will happen. No permanents will be exiled, including " +
                "those with the same name as the target."
        )
        ruling(
            "2012-10-01",
            "If Detention Sphere leaves the battlefield before its \"enters\" ability has resolved, its " +
                "leaves-the-battlefield ability will trigger and do nothing. Then the \"enters\" ability will " +
                "resolve and exile the targeted nonland permanent and other permanents with that name indefinitely."
        )
    }
}
