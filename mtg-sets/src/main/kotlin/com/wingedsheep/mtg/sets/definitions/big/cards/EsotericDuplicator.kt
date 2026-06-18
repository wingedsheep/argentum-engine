package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Esoteric Duplicator
 * {2}{U}
 * Artifact — Clue
 *
 * Whenever you sacrifice this artifact or another artifact, you may pay {2}. If you do, at the
 * beginning of the next end step, create a token that's a copy of that artifact.
 * {2}, Sacrifice this artifact: Draw a card.
 *
 * The sacrifice trigger uses [Triggers.YouSacrificeOneOrMore] with an Artifact filter and ANY
 * binding. Per CR, "whenever you sacrifice this artifact or another artifact" includes the source
 * sacrificing itself — the engine's self-sacrifice detection now fires ANY-binding sacrifice-batch
 * triggers off the just-sacrificed permanent, so this single ability covers both "this" and
 * "another". "That artifact" is bound as the triggering entity; the delayed end-step token-copy
 * captures its id at creation time and copies the artifact's printed characteristics via last-known
 * information (it's already in the graveyard by the time the copy is made).
 *
 * The Clue ability ({2}, Sacrifice: Draw a card) is itself a sacrifice that feeds the trigger.
 */
val EsotericDuplicator = card("Esoteric Duplicator") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Clue"
    oracleText = "Whenever you sacrifice this artifact or another artifact, you may pay {2}. If you " +
        "do, at the beginning of the next end step, create a token that's a copy of that artifact.\n" +
        "{2}, Sacrifice this artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.YouSacrificeOneOrMore(GameObjectFilter.Artifact)
        effect = MayPayManaEffect(
            ManaCost.parse("{2}"),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = Effects.CreateTokenCopyOfTarget(target = EffectTarget.TriggeringEntity)
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3dbb2755-97d9-492e-8697-5548160678c8.jpg?1739804161"
    }
}
