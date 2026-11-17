package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elite Scaleguard — Fate Reforged #12 (canonical printing)
 * {4}{W} · Creature — Human Soldier · 2/3
 *
 * When this creature enters, bolster 2.
 * Whenever a creature you control with a +1/+1 counter on it attacks, tap target creature
 * defending player controls.
 *
 * The attack trigger is Byrke, Long Ear of the Law's `withCounter` attacker filter. Its target is
 * a creature controlled by the player *that attacker* is attacking: the per-attacker
 * [Player.DefendingPlayer] reads the triggering attacker's defender first (Mage-Ring Responder
 * shape), so in multiplayer each qualifying attacker taps a creature of its own defending player.
 */
val EliteScaleguard = card("Elite Scaleguard") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, bolster 2. (Choose a creature with the least toughness among " +
        "creatures you control and put two +1/+1 counters on it.)\n" +
        "Whenever a creature you control with a +1/+1 counter on it attacks, tap target creature " +
        "defending player controls."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Mechanic.bolster(2)
    }

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)
        ).attacks()
        val creature = target(
            TargetFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
        )
        effect = Effects.Tap(creature)
        description = "Whenever a creature you control with a +1/+1 counter on it attacks, tap target " +
            "creature defending player controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55ac3224-54af-4688-a015-6a447971544e.jpg?1783938713"
        ruling("2014-11-24", "Bolster itself doesn't target any creature, though some spells and abilities that bolster may have other effects that target creatures. For example, you could put counters on a creature with protection from white with Abzan Skycaptain's bolster ability.")
        ruling("2014-11-24", "You determine which creature to put counters on as the spell or ability that instructs you to bolster resolves.")
    }
}
