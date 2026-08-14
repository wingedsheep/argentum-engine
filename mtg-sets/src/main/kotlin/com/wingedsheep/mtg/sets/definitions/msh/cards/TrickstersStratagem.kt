package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Trickster's Stratagem
 * {3}{U}
 * Sorcery
 *
 * The owner of target creature an opponent controls puts it into their library second from the top
 * or on the bottom. Then up to one target creature you control connives.
 *
 * Two independent targets chosen at cast time (CR 601.2c):
 *  - The removal half is [Effects.PutSecondFromTopOrBottomOfLibrary] — the owner of the targeted
 *    creature picks second-from-top or bottom as it resolves (the Temporal Cleansing shape).
 *  - The connive half copies Unstable Experiment: "up to one target creature you control connives"
 *    means *that creature* is the source of the connive keyword action (CR 701.50), so when the
 *    optional target was declined — or the chosen creature has left the battlefield by resolution —
 *    nothing connives and you neither draw nor discard. Hence the [ConditionalEffect] gate on the
 *    second target slot actually holding a creature, rather than an unconditional connive.
 */
val TrickstersStratagem = card("Trickster's Stratagem") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "The owner of target creature an opponent controls puts it into their library " +
        "second from the top or on the bottom. Then up to one target creature you control " +
        "connives. (Draw a card, then discard a card. If you discarded a nonland card, put a " +
        "+1/+1 counter on that creature.)"

    spell {
        val victim = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        val conniver = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl),
        )
        effect = Effects.PutSecondFromTopOrBottomOfLibrary(victim) then ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature, targetIndex = 1),
            effect = Effects.Connive(conniver),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "David Palumbo"
        flavorText = "\"Let's take you off the board, shall we?\""
        imageUri = "https://cards.scryfall.io/normal/front/6/2/620376d0-dc0c-405f-8121-eb36d9b4f4c2.jpg?1783902948"
    }
}
