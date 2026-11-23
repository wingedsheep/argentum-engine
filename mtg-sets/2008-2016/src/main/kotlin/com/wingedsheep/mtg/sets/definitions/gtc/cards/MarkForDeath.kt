package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mark for Death
 * {3}{R}
 * Sorcery
 * Target creature an opponent controls blocks this turn if able. Untap that creature. Other
 * creatures that player controls can't block this turn.
 *
 * "Other creatures that player controls" is a live group (a creature that player gets later this
 * turn can't block either — "can't block" changes no characteristics, CR 611.2c). The player and
 * the excluded target are pinned when the spell resolves.
 */
val MarkForDeath = card("Mark for Death") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature an opponent controls blocks this turn if able. Untap that creature. " +
        "Other creatures that player controls can't block this turn."

    spell {
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.MarkMustBlockThisTurn(creature) then
            Effects.Untap(creature) then
            Effects.CantBlockGroup(
                GroupFilter(GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController))
                    .otherThanTarget()
            )
    }

    metadata {
        ruling("2013-01-24", "That creature's controller still decides which attacking creature it blocks.")
        ruling(
            "2013-01-24",
            "If the creature can't block (perhaps because it has become tapped or it doesn't have flying and all " +
                "attacking creatures do), then the requirement to block does nothing."
        )
        ruling(
            "2013-01-24",
            "If there are any costs required for that creature to block (such as the one imposed by War Cadence), " +
                "the creature's controller isn't required to pay those costs. In that case, the requirement to " +
                "block would do nothing."
        )
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Mathias Kollros"
        flavorText = "Everyone roots for the little guy, but nobody bets on him."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45498dbd-a512-4299-800b-06c15a4fd94e.jpg?1783940123"
    }
}
