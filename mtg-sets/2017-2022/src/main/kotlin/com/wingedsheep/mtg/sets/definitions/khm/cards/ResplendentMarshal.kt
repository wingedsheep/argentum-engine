package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Resplendent Marshal — Kaldheim #22
 * {1}{W}{W} · Creature — Angel Warrior · 3/3
 *
 * Flying
 * When this creature enters or dies, you may exile another creature card from your graveyard.
 * When you do, put a +1/+1 counter on each creature you control other than this creature that
 * shares a creature type with the exiled card.
 *
 * Shape notes:
 *  - "Enters or dies" is two triggered abilities sharing one effect.
 *  - The exile is the optional action of a CR 603.12 reflexive trigger (Kishla Trawlers shape): the
 *    card is chosen on resolution through a non-targeting `selectTarget`, so the "may" is skipped
 *    entirely when no other creature card is in the graveyard. `.other()` keeps the Marshal from
 *    exiling itself when the dies trigger resolves with it in the graveyard.
 *  - The reflexive payoff inherits the action's pipeline, so the exiled card is read back through
 *    `PipelineTarget(EXILED)`; its creature types are its printed ones in exile (Changeling honoured).
 *  - "Other than this creature" is `excludeSelf`; after a death the Marshal isn't on the
 *    battlefield to match anyway.
 */
/** Pipeline slot holding the card exiled by the action half, read back by the reflexive payoff. */
private const val EXILED = "resplendentMarshalExiled"

private val exileThenCounters: Effect = Effects.ReflexiveTrigger(
    action = Effects.Pipeline {
        val exiled = selectTarget(
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard.other()),
            nonTargeting = true,
            name = EXILED,
            prompt = "Choose another creature card in your graveyard to exile"
        )
        run(Effects.Exile(exiled.asTarget))
    },
    reflexiveEffect = Effects.ForEachInGroup(
        GroupFilter(
            GameObjectFilter.Creature.youControl()
                .sharingCreatureTypeWith(EffectTarget.PipelineTarget(EXILED)),
            excludeSelf = true
        ),
        Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
    ),
    optional = true,
    descriptionOverride = "You may exile another creature card from your graveyard. When you do, " +
        "put a +1/+1 counter on each creature you control other than this creature that shares a " +
        "creature type with the exiled card."
)

val ResplendentMarshal = card("Resplendent Marshal") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Warrior"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters or dies, you may exile another creature card from your graveyard. " +
        "When you do, put a +1/+1 counter on each creature you control other than this creature that " +
        "shares a creature type with the exiled card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = exileThenCounters
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = exileThenCounters
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "22"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e849bec-2ee6-4c83-83e6-bf9f5543e414.jpg?1783928279"
        ruling(
            "2021-02-05",
            "You don't choose which creature card you're exiling, if any, until the triggered ability is " +
                "resolving. When you do, the reflexive triggered ability triggers. Players may respond to " +
                "this reflexive triggered ability, knowing the creature types of the card you exiled and " +
                "which creatures might get a +1/+1 counter."
        )
    }
}
