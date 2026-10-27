package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ZoneChangeCause
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Orvar, the All-Form (KHM #70)
 * {3}{U}
 * Legendary Creature — Shapeshifter
 * 3/3
 *
 * - The cast trigger gates on [SpellCastPredicate.TargetsMatching] with "other permanents you
 *   control" read relative to Orvar, and captures exactly those targets as the trigger fires — so
 *   the spell may also target other things, but only the permanents you control that it targets
 *   can be copied (ruling 2021-02-05).
 * - The intervening "if" is re-checked on resolution by narrowing the capture to the targets still
 *   on the battlefield: if all of them have left, nothing is created; if one is left, it's copied
 *   even if it's now an illegal target for the spell. With several, you choose which one to copy
 *   as the token is created.
 * - The discard trigger is the self-bound discard trigger narrowed to an opponent's spell or
 *   ability ([ZoneChangeCause.DiscardedByOpponentEffect]); cleanup-step and cost discards never
 *   fire it. Its target is any permanent, regardless of controller.
 */
val OrvarTheAllForm = card("Orvar, the All-Form") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Shapeshifter"
    power = 3
    toughness = 3
    oracleText = "Changeling\n" +
        "Whenever you cast an instant or sorcery spell, if it targets one or more other permanents you " +
        "control, create a token that's a copy of one of those permanents.\n" +
        "When a spell or ability an opponent controls causes you to discard this card, create a token " +
        "that's a copy of target permanent."

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery,
            requires = setOf(
                SpellCastPredicate.TargetsMatching(
                    GameObjectFilter.Permanent.youControl().notSourceItself()
                )
            )
        )
        effect = Effects.Pipeline {
            val stillThere = filter(triggerCaptured, GameObjectFilter.Any.currentlyIn(Zone.BATTLEFIELD))
            val chosen = chooseExactly(
                1,
                from = stillThere,
                prompt = "Choose a permanent to copy",
                useTargetingUI = true
            )
            run(Effects.ForEachInCollection(chosen, Effects.CreateTokenCopyOfTarget(EffectTarget.IterationEntity)))
        }
    }

    triggeredAbility {
        trigger = Triggers.self.isDiscarded(cause = ZoneChangeCause.DiscardedByOpponentEffect)
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.CreateTokenCopyOfTarget(permanent)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "70"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f36775e-9e48-49cc-a771-d58481712edc.jpg?1783928258"

        ruling("2021-02-05", "For the first triggered ability, the instant or sorcery spell can target other things if it also targets one or more permanents you control; however, the token you create can copy only permanents you control that are targets of the spell. If the spell targets more than one permanent you control, you choose which permanent the token is copying as you create the token.")
        ruling("2021-02-05", "As the first triggered ability tries to resolve, the spell that caused it to trigger and that spell's targets are checked again. Ignore any target of the spell that has left the battlefield by that point. If all of the permanents you controlled that were targets have left the battlefield by that point, the triggered ability will do nothing and no token will be created. If at least one permanent you control that's a target is still on the battlefield, the triggered ability will resolve, even if that permanent is now an illegal target for the spell.")
        ruling("2021-02-05", "For the second triggered ability, you can choose any permanent as the target, regardless of who controls it.")
        ruling("2021-02-05", "If a spell or ability an opponent controls allows or instructs you to choose a card to discard, and you discard Orvar, its last ability will trigger.")
    }
}
