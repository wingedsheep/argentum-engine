package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sudden Storm — Born of the Gods #53 (canonical printing)
 * {3}{U} · Instant
 *
 * Tap up to two target creatures. Those creatures don't untap during their controllers' next
 * untap steps. Scry 1.
 *
 * Will of the Naga's tap-and-freeze body followed by Scry 1: each chosen creature is tapped and
 * gets DOESNT_UNTAP keyed to its own (current) controller's next untap step.
 */
val SuddenStorm = card("Sudden Storm") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures. Those creatures don't untap during their controllers' next untap steps. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        ) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Adam Paquette"
        flavorText = "No matter how big and strong you are, the sea is bigger and stronger."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6beffc33-58fd-4364-b87b-d4573ecaf4bd.jpg?1783939565"
        ruling("2014-02-01", "If you choose zero targets, you'll just scry 1 when the ability resolves. However, if you choose at least one target and all of Sudden Storm's targets are illegal as it tries to resolve, the spell won't resolve and none of its effects will happen. You won't scry in that case.")
        ruling("2014-02-01", "If a creature affected by Sudden Storm changes controllers before its old controller's next untap step, Sudden Storm will prevent it from becoming untapped during its new controller's next untap step.")
        ruling("2014-02-01", "This spell can target tapped creatures. If a targeted creature is already tapped when the spell resolves, that creature remains tapped and doesn't untap during its controller's next untap step.")
        ruling("2014-02-01", "If you chose two targets and one is an illegal target when Sudden Storm resolves, that creature won't become tapped and it won't be stopped from untapping during its controller's next untap step. It won't be affected by Sudden Storm in any way.")
    }
}
