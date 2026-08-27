package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Vine Snare
 * {2}{G}
 * Instant
 * Prevent all combat damage that would be dealt this turn by creatures with power 4 or less.
 */
val VineSnare = card("Vine Snare") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn by creatures with power 4 or less."

    spell {
        effect = PreventDamageEffect(
            scope = PreventionScope.CombatOnly,
            direction = PreventionDirection.FromTarget,
            sourceFilter = PreventionSourceFilter.FromGroup(
                GroupFilter(GameObjectFilter.Creature.powerAtMost(4))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "205"
        artist = "Igor Kieryluk"
        flavorText = "Nissa found that the vines of the marsh could ensnare just as well as forest vines could—maybe even better."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2241f71-4319-49bf-905a-b6b774ffcb27.jpg?1783938317"

        ruling("2015-06-22", "Check the power of each creature as it would deal combat damage to determine if that damage is prevented. It doesn't matter what any creature's power is as Vine Snare resolves.")
    }
}
