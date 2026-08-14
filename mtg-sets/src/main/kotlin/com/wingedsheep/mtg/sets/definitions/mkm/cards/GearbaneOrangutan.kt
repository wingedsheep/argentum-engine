package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Gearbane Orangutan — Murders at Karlov Manor #129
 * {2}{R} · Creature — Ape · 2/2
 *
 * Reach
 * When this creature enters, choose one —
 * • Destroy up to one target artifact.
 * • Sacrifice an artifact. If you do, put two +1/+1 counters on this creature.
 *
 * The mode is chosen as the trigger goes on the stack (CR 601.2b via 603.3d), and both modes are
 * always legal choices: mode 1 is "up to one" so zero targets is legal, mode 2 takes no target.
 * That matters for a board with no artifacts at all — you may still pick either mode and simply
 * accomplish nothing.
 *
 * Mode 2's sacrifice is **mandatory, not a "may"**, so it is [Effects.IfYouDo] rather than a
 * `MayEffect`: with no artifact to sacrifice nothing is sacrificed and the counters don't happen.
 * [SuccessCriterion.PermanentsSacrificed] is the criterion that reads that correctly — `Auto`
 * can't infer a sacrifice (which graveyard it lands in isn't known until the chooser picks) and
 * `Always` would fail open and hand out counters for free. Gearbane Orangutan is itself a
 * creature, not an artifact, so it can never be the artifact it sacrifices.
 */
val GearbaneOrangutan = card("Gearbane Orangutan") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ape"
    power = 2
    toughness = 2
    oracleText = "Reach\n" +
        "When this creature enters, choose one —\n" +
        "• Destroy up to one target artifact.\n" +
        "• Sacrifice an artifact. If you do, put two +1/+1 counters on this creature."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                TargetPermanent(optional = true, filter = TargetFilter.Artifact),
                "Destroy up to one target artifact"
            ),
            Mode.noTarget(
                Effects.IfYouDo(
                    action = Effects.Sacrifice(
                        GameObjectFilter.Artifact,
                        count = 1,
                        target = EffectTarget.Controller
                    ),
                    ifYouDo = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
                    successCriterion = SuccessCriterion.PermanentsSacrificed
                ),
                "Sacrifice an artifact — if you do, put two +1/+1 counters on this creature"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Svetlin Velinov"
        flavorText = "\"In her defense, thopters are very fun to smash.\"\n—Udol of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6900a344-a155-4ee1-a3ac-d6c28e024270.jpg?1783912881"
    }
}
