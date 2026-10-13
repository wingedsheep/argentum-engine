package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Temporal Firestorm
 * {3}{R}{R}
 * Sorcery
 * Kicker {1}{W} and/or {1}{U}
 * Choose up to X creatures and/or planeswalkers you control, where X is the number of times this
 * spell was kicked. Those permanents phase out.
 * Temporal Firestorm deals 5 damage to each creature and each planeswalker.
 *
 * - "Kicker {1}{W} and/or {1}{U}" is two kicker abilities in printed order (Urborg Lhurgoyf's
 *   shape), so X — "the number of times this spell was kicked" — is 0, 1 or 2, summed from the two
 *   linked kicker slots the resolving spell carries.
 * - The choice isn't targeting: it's made on resolution, a Gather → chooseUpTo(X) over your own
 *   creatures and planeswalkers, then each chosen permanent phases out. Unkicked, X is 0 and nothing
 *   is asked.
 * - The sweep runs after the phase-out, and phased-out permanents are treated as though they don't
 *   exist (CR 702.26b), so the chosen ones are spared. They phase back in during your next untap step.
 */
val TemporalFirestorm = card("Temporal Firestorm") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "WUR"
    typeLine = "Sorcery"
    oracleText = "Kicker {1}{W} and/or {1}{U} (You may pay an additional {1}{W} and/or {1}{U} as you cast this spell.)\n" +
        "Choose up to X creatures and/or planeswalkers you control, where X is the number of times this spell " +
        "was kicked. Those permanents phase out.\n" +
        "Temporal Firestorm deals 5 damage to each creature and each planeswalker."

    keywordAbility(KeywordAbility.kicker("{1}{W}"))
    keywordAbility(KeywordAbility.kicker("{1}{U}"))

    spell {
        effect = Effects.Pipeline {
            val yours = gather(GameObjectFilter.CreatureOrPlaneswalker, player = Player.You)
            val sheltered = chooseUpTo(
                DynamicAmounts.conditional(Conditions.WasKickedWithFirstKicker, 1, 0) +
                    DynamicAmounts.conditional(Conditions.WasKickedWithSecondKicker, 1, 0),
                from = yours,
                prompt = "Choose creatures and/or planeswalkers you control to phase out",
                selectedLabel = "Phase out",
                remainderLabel = "Stay",
                useTargetingUI = true,
            )
            run(Effects.ForEachInCollection(sheltered, Effects.PhaseOut(EffectTarget.IterationEntity)))
            run(
                Patterns.Group.dealDamageToAll(
                    amount = 5,
                    filter = GroupFilter(GameObjectFilter.CreatureOrPlaneswalker),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Néstor Ossandón Leal"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d690340-75f1-4733-bde5-2d60fd02f060.jpg?1783921309"

        ruling(
            "2022-09-09",
            "You may kick Temporal Firestorm only once for its {1}{W} cost and only once for its {1}{U} cost. You can kick it twice by paying {2}{W}{U}, but you can't kick it twice by paying {2}{W}{W} or {2}{U}{U}. You can't kick it more than twice. Notably, this means that X will always be either one or two if Temporal Firestorm was kicked."
        )
    }
}
