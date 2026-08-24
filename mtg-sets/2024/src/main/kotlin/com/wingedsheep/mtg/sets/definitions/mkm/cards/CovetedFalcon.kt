package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Coveted Falcon — Murders at Karlov Manor #48
 * {1}{U}{U} · Artifact Creature — Bird · 1/4
 *
 * Flying
 * Whenever this creature attacks, gain control of target permanent you own but don't control.
 * Disguise {1}{U}
 * When this creature is turned face up, target opponent gains control of any number of target
 * permanents you control. Draw a card for each one they gained control of this way.
 *
 * A card that hands permanents out and then takes them back, so both halves are control changes
 * read from opposite ends.
 *
 * **"You own but don't control" is a composed controller predicate**, not a named one:
 * `ControllerPredicate.And(OwnedByYou, Not(ControlledByYou))`. The two axes are genuinely
 * different — ownership is the card's immutable owner, control is projected and moves with
 * Layer-2 effects — and `GameObjectFilter.and` deliberately rejects two *different* controller
 * predicates rather than silently keeping one, so the intent has to be stated as one composed
 * predicate. `Not(ControlledByYou)` rather than `ControlledByOpponent` because that is what the
 * card prints; on the battlefield the two coincide, but the negation is the wording and doesn't
 * depend on every other player being an opponent.
 *
 * **The gift is a per-permanent gate, not one bulk effect.** "Draw a card for each one they gained
 * control of **this way**" counts control changes that actually happened, not targets chosen — a
 * permanent that left the battlefield (or that you no longer control) between trigger and
 * resolution is skipped, and it must not draw. So the chosen permanent targets are gathered with
 * [CardSource.ChosenTargets] and iterated with [ForEachInCollectionEffect], and each iteration is
 * the same [SuccessCriterion.ControlChanged] gate Stiltzkin, Moogle Merchant uses for its singular
 * "If they do, you draw a card" — one draw per control change that really moved. The gather skips
 * player targets by construction, so the target opponent in slot 0 is never iterated over.
 *
 * `unlimited = true` is the right shape for "any number of target permanents" — these *are*
 * targets (shroud, protection and "can't be the target of" all apply, and each is locked in when
 * the trigger goes on the stack), unlike a resolution-time "choose any number" pipeline. Choosing
 * zero is legal, and then the trigger simply draws nothing.
 *
 * The turned-face-up trigger is not an enters trigger: turning a permanent face up doesn't cause
 * enters-the-battlefield abilities to trigger (CR 708.8), so a hard-cast Falcon never gives
 * anything away — you only get the gift-and-draw by disguising it and flipping it up.
 */
val CovetedFalcon = card("Coveted Falcon") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Bird"
    oracleText = "Flying\n" +
        "Whenever this creature attacks, gain control of target permanent you own but don't " +
        "control.\n" +
        "Disguise {1}{U} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, target opponent gains control of any number of " +
        "target permanents you control. Draw a card for each one they gained control of this way."
    power = 1
    toughness = 4
    keywords(Keyword.FLYING)

    disguise = "{1}{U}"

    triggeredAbility {
        trigger = Triggers.Attacks
        val stolen = target(
            "target permanent you own but don't control",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Permanent.withControllerPredicate(
                        ControllerPredicate.And(
                            listOf(
                                ControllerPredicate.OwnedByYou,
                                ControllerPredicate.Not(ControllerPredicate.ControlledByYou),
                            )
                        )
                    )
                )
            ),
        )
        effect = Effects.GainControl(stolen)
        description = "Whenever this creature attacks, gain control of target permanent you own " +
            "but don't control."
    }

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val opponent = target("target opponent", TargetOpponent())
        target(
            "any number of target permanents you control",
            TargetPermanent(
                unlimited = true,
                filter = TargetFilter(GameObjectFilter.Permanent.youControl()),
            ),
        )
        effect = Effects.Composite(
            GatherCardsEffect(source = CardSource.ChosenTargets, storeAs = "falconGifts"),
            ForEachInCollectionEffect(
                collection = "falconGifts",
                effect = IfYouDoEffect(
                    action = GiveControlToTargetPlayerEffect(
                        permanent = EffectTarget.Self,
                        newController = opponent,
                    ),
                    ifYouDo = Effects.DrawCards(1),
                    successCriterion = SuccessCriterion.ControlChanged,
                ),
            ),
        )
        description = "When this creature is turned face up, target opponent gains control of any " +
            "number of target permanents you control. Draw a card for each one they gained " +
            "control of this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Madeline Boni"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc936987-d58b-4e7c-870f-379bcae77727.jpg?1783912913"

        ruling("2024-02-02", "A token's owner is the player who created it.")
        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
        ruling(
            "2024-02-02",
            "A permanent that turns face up or face down changes characteristics but is otherwise " +
                "the same permanent. Spells and abilities that were targeting that permanent and " +
                "Auras and Equipment that were attached to that permanent aren't affected unless " +
                "the new characteristics of the object change the legality of those targets or " +
                "attachments."
        )
    }
}
