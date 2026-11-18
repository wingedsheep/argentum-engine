package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ob Nixilis, Unshackled
 * {4}{B}{B}
 * Legendary Creature — Demon
 * 4/4
 * Flying, trample
 * Whenever an opponent searches their library, that player sacrifices a creature of their choice
 * and loses 10 life.
 * Whenever another creature dies, put a +1/+1 counter on Ob Nixilis.
 *
 * "That player" is the searcher: the library-search trigger binds them as
 * [Player.TriggeringPlayer]. Both halves happen even if they have no creature to sacrifice.
 */
val ObNixilisUnshackled = card("Ob Nixilis, Unshackled") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon"
    power = 4
    toughness = 4
    oracleText = "Flying, trample\n" +
        "Whenever an opponent searches their library, that player sacrifices a creature of their choice and loses 10 life.\n" +
        "Whenever another creature dies, put a +1/+1 counter on Ob Nixilis."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.anOpponent.searchesLibrary()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.LoseLife(10, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "That player sacrifices a creature of their choice and loses 10 life."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Put a +1/+1 counter on Ob Nixilis."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/109b28dc-c0d1-4b88-b18e-074ac0464e03.jpg?1783939181"
    }
}
