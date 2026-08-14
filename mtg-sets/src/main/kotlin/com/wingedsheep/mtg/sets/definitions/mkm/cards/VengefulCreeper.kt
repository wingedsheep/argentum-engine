package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Vengeful Creeper — Murders at Karlov Manor #182
 * {4}{G} · Creature — Plant Elemental · 5/5
 *
 * Disguise {5}{G}
 * When this creature is turned face up, destroy target artifact or enchantment an opponent controls.
 *
 * The Naturalize is locked behind the flip, not the cast: hard-casting for {4}{G} gets a vanilla 5/5
 * and no removal at all, because turning face up is not entering the battlefield (CR 702.168d) and
 * this isn't an enters trigger anyway. The disguise line is the mode that matters — {3} for a 2/2
 * with ward {2}, then {5}{G} at instant speed to blow up an artifact or enchantment as a surprise.
 *
 * Turning face up is a special action (CR 702.168c): it uses no stack and can't be responded to, so
 * the opponent's only window to save the permanent is after the *trigger* goes on the stack, with the
 * 5/5 already face up. If the target becomes illegal before the trigger resolves it fizzles; the
 * creature stays face up regardless.
 */
val VengefulCreeper = card("Vengeful Creeper") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    oracleText = "Disguise {5}{G} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, destroy target artifact or enchantment an opponent controls."
    power = 5
    toughness = 5
    disguise = "{5}{G}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = TargetObject(filter = TargetFilter.ArtifactOrEnchantment.opponentControls())
        effect = Effects.Destroy(EffectTarget.ContextTarget(0))
        description = "When this creature is turned face up, destroy target artifact or enchantment an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Maria Poliakova"
        flavorText = "Seeking intact evidence in the Rubblebelt is a fool's errand."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a914416-effd-4eda-b609-2773c53a08ec.jpg?1783912858"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
    }
}
