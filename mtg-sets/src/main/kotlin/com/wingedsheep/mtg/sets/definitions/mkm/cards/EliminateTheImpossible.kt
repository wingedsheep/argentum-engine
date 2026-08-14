package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eliminate the Impossible — Murders at Karlov Manor #54
 * {1}{U} · Instant
 *
 * Investigate. Creatures your opponents control get -2/-0 until end of turn. If any of them are
 * suspected, they're no longer suspected.
 *
 * A combat trick pointed the wrong way: it blunts an attack *and* strips the menace / can't-block
 * package off every suspected attacker, turning the opponent's own suspect payoffs against them.
 *
 * All three sentences are untargeted and resolve together, so this is one [Effects.then] chain in
 * a single `spell`. Per the printed ruling the affected set is locked in at resolution — creatures
 * an opponent gains control of later in the turn get neither the -2/-0 nor the absolution, which
 * is what both the group modification and the [Effects.ForEachInGroup] sweep give: each reads the
 * battlefield once, as the spell resolves.
 *
 * "If any of them are suspected" is scoped to the same set as the -2/-0, hence
 * `Creature.opponentControls().suspected()` rather than [AbsolvingLammasu]'s symmetric sweep over
 * every suspected creature — your own suspected attackers keep their menace.
 *
 * [Effects.NoLongerSuspected] (CR 701.60c) removes the whole suspect application at once: status,
 * menace, and "this creature can't block" all hang off being suspected, so all three go together.
 * Menace from any other source is untouched.
 */
val EliminateTheImpossible = card("Eliminate the Impossible") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Investigate. Creatures your opponents control get -2/-0 until end of turn. If " +
        "any of them are suspected, they're no longer suspected. (To investigate, create a Clue " +
        "token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        effect = Effects.Investigate()
            .then(
                Patterns.Group.modifyStatsForAll(
                    power = -2,
                    toughness = 0,
                    filter = GroupFilter.AllCreaturesOpponentsControl
                )
            )
            .then(
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.opponentControls().suspected()),
                    Effects.NoLongerSuspected(EffectTarget.Self)
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Carlos Palma Cruchaga"
        flavorText = "\"Half the city had motive. Time to narrow down who also had the means and " +
            "opportunity.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/8/486f1cc2-c162-448e-91a9-577d7d796584.jpg?1783912910"

        ruling(
            "2024-02-02",
            "Eliminate the Impossible affects only creatures your opponents control at the time " +
                "it resolves. Creatures they begin to control later in the turn won't get -2/-0 " +
                "and won't stop being suspected."
        )
        ruling(
            "2024-02-02",
            "When an effect suspects a creature, it becomes suspected. It gains menace and \"This " +
                "creature can't block\" for as long as it's suspected. It stays suspected until it " +
                "leaves the battlefield or another effect causes it to no longer be suspected."
        )
        ruling(
            "2024-02-02",
            "If a suspected creature loses all abilities, it will lose menace and \"This creature " +
                "can't block\", but it won't stop being suspected."
        )
    }
}
