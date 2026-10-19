package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Spirit-Sister's Call
 * {3}{W}{B}
 * Enchantment
 * At the beginning of your end step, choose target permanent card in your graveyard. You may
 * sacrifice a permanent that shares a card type with the chosen card. If you do, return the chosen
 * card from your graveyard to the battlefield and it gains "If this permanent would leave the
 * battlefield, exile it instead of putting it anywhere else."
 *
 * The optional sacrifice is Midgar, City of Mako's shape: an [Effects.IfYouDo] over a pipeline that
 * offers the matching permanents with `chooseUpTo(1)` (choosing none is declining); "if you do"
 * reads that choice, since a sacrificed permanent exiled by its own replacement was still sacrificed. The fodder filter is
 * `sharingCardTypeWith(target)`, read against the graveyard card at resolution (Reweave reads the
 * same predicate against a bound target). The granted replacement is [Effects.GrantExileOnLeave],
 * as on Geth, Thane of Contracts — no duration, so it outlives Spirit-Sister's Call itself.
 */
val SpiritSistersCall = card("Spirit-Sister's Call") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, choose target permanent card in your graveyard. " +
        "You may sacrifice a permanent that shares a card type with the chosen card. If you do, return " +
        "the chosen card from your graveyard to the battlefield and it gains \"If this permanent would " +
        "leave the battlefield, exile it instead of putting it anywhere else.\""

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val chosen = target(TargetFilter.PermanentInYourGraveyard)
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val fodder = gather(
                    GameObjectFilter.Permanent.sharingCardTypeWith(chosen),
                    player = Player.You
                )
                val picked = chooseUpTo(
                    1,
                    from = fodder,
                    useTargetingUI = true,
                    prompt = "You may sacrifice a permanent that shares a card type with the chosen card",
                    name = "sacrificed"
                )
                sacrifice(picked)
            },
            then = Effects.PutOntoBattlefieldFromGraveyard(chosen) then
                Effects.GrantExileOnLeave(chosen),
            // Named: a sacrificed permanent that is exiled instead (an earlier return of this
            // card) was still sacrificed, so "if you do" reads the choice, not the graveyard.
            successCriterion = SuccessCriterion.CollectionNonEmpty("sacrificed")
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "237"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f094735f-5843-4f75-bc90-3e248a061a43.jpg?1783923829"
        ruling(
            "2022-02-18",
            "If Spirit-Sister's Call leaves the battlefield, the replacement effect continues to apply. " +
                "If one of the permanents it returned would leave the battlefield, it'll be exiled instead."
        )
        ruling(
            "2022-02-18",
            "Because to die means to be put into a graveyard from the battlefield, a creature or " +
                "planeswalker that is exiled instead doesn't \"die.\" Abilities that would trigger when " +
                "it dies won't trigger."
        )
    }
}
