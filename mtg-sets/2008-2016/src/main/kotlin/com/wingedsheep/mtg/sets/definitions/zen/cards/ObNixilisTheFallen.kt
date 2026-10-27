package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ob Nixilis, the Fallen — Zendikar #107 (canonical printing)
 * {3}{B}{B} · Legendary Creature — Demon · 3/3
 *
 * Landfall — Whenever a land you control enters, you may have target player lose 3 life. If you
 * do, put three +1/+1 counters on Ob Nixilis.
 *
 * The target is chosen as the trigger goes on the stack; the printed "you may" is the builder's
 * `optional = true` (a `Gate.MayDecide` on resolution) wrapping both the life loss and the
 * counters, so declining skips both and accepting does both — "if you do" is the accept branch.
 */
val ObNixilisTheFallen = card("Ob Nixilis, the Fallen") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon"
    power = 3
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, you may have target player lose 3 life. " +
        "If you do, put three +1/+1 counters on Ob Nixilis."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        optional = true
        val player = target(Targets.Player)
        effect = Effects.LoseLife(3, player) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "107"
        artist = "Jason Felix"
        flavorText = "His spark lost, he plots revenge upon the plane whose corrupting mana fractured his soul."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7824a001-3f0e-4ac5-8406-e58f4288f2f2.jpg?1783942149"
    }
}
