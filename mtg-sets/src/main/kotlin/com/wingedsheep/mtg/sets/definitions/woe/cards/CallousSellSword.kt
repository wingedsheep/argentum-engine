package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker

/**
 * Callous Sell-Sword // Burn Together
 * {1}{B}
 * Creature — Human Soldier
 * 2/2
 * This creature enters with a +1/+1 counter on it for each creature that died under your control
 * this turn.
 *
 * Adventure: Burn Together — {R}, Sorcery — Adventure
 * Target creature you control deals damage equal to its power to any other target. Then sacrifice it.
 *
 * The counters are an [EntersWithDynamicCounters] replacement (CR 614.1c), not an enters trigger, so
 * they are already on it the instant it hits the battlefield — that matters because the intended line
 * is casting Burn Together first and then the creature, and the sacrificed creature has to be counted.
 * `TurnTracking(Player.You, CREATURES_DIED)` is the per-player turn tracker, which counts tokens too
 * ("each creature that died under your control", not "each creature card"). Leaving `otherOnly` false
 * keeps it self-scoped: the global scan only applies `EntersWithDynamicCounters` when `otherOnly` is
 * set, so your other creatures don't pick counters up from it.
 *
 * Burn Together is the Self-Destruct shape — the chosen creature (target index 0) is the
 * `damageSource`, so the damage is dealt *by it* (deathtouch, lifelink and damage-redirection all
 * read off it). "Any other target" is [TargetOther] over [AnyTarget], which enforces distinctness
 * from target 0 rather than merely offering a second any-target.
 *
 * Both rulings about illegal targets fall out of the ordering: if the creature is gone the damage
 * step has no source and the sacrifice finds nothing, and if only the second target is gone the
 * sacrifice still happens.
 */
val CallousSellSword = card("Callous Sell-Sword") {
    manaCost = "{1}{B}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Soldier"
    oracleText = "This creature enters with a +1/+1 counter on it for each creature that died " +
        "under your control this turn."
    power = 2
    toughness = 2

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.TurnTracking(Player.You, TurnTracker.CREATURES_DIED),
        )
    )

    adventure("Burn Together") {
        manaCost = "{R}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target creature you control deals damage equal to its power to any other " +
            "target. Then sacrifice it. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val yourCreature = target("target creature you control", Targets.CreatureYouControl)
            val other = target("any other target", TargetOther(baseRequirement = AnyTarget()))
            effect = Effects.DealDamage(
                DynamicAmounts.targetPower(0),
                other,
                damageSource = yourCreature,
            ) then Effects.SacrificeTarget(yourCreature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/770ee3da-d33e-466f-9a2e-ad2d08ef5012.jpg?1783915066"

        ruling(
            "2023-09-01",
            "If the first target of Burn Together is an illegal target as the spell resolves but " +
                "the last target is still legal, Burn Together will resolve, but no damage will be " +
                "dealt and nothing will be sacrificed."
        )
        ruling(
            "2023-09-01",
            "If the last target of Burn Together is an illegal target as the spell resolves but " +
                "the first target is still legal, Burn Together will still resolve and you'll " +
                "still sacrifice the first target."
        )
    }
}
