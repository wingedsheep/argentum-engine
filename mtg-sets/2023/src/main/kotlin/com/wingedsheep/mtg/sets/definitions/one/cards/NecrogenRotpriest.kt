package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Necrogen Rotpriest
 * {2}{B}{G}
 * Creature — Phyrexian Zombie Cleric
 * 1/5
 * Toxic 2
 * Whenever a creature you control with toxic deals combat damage to a player, that player gets an
 * additional poison counter.
 * {1}{B}{G}: Target creature you control with toxic gains deathtouch until end of turn.
 *
 * The combat-damage trigger is per damaging creature (not a batch), and includes the Rotpriest
 * itself. "That player" is the damaged player ([Player.TriggeringPlayer]). "With toxic" is
 * `withKeyword(TOXIC)`, matching printed or granted toxic N off the projected `TOXIC_<n>` keyword.
 */
val NecrogenRotpriest = card("Necrogen Rotpriest") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Phyrexian Zombie Cleric"
    power = 1
    toughness = 5
    oracleText = "Toxic 2 (Players dealt combat damage by this creature also get two poison counters.)\n" +
        "Whenever a creature you control with toxic deals combat damage to a player, that player " +
        "gets an additional poison counter.\n" +
        "{1}{B}{G}: Target creature you control with toxic gains deathtouch until end of turn."

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 2))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withKeyword(Keyword.TOXIC))
            .dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(
            CounterType.POISON,
            1,
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
        description = "Whenever a creature you control with toxic deals combat damage to a player, " +
            "that player gets an additional poison counter."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}{G}")
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.youControl().withKeyword(Keyword.TOXIC))
        )
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
        description = "Target creature you control with toxic gains deathtouch until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87519b36-1d95-4020-a61d-6afb75555e3e.jpg?1783917998"
        ruling(
            "2023-02-04",
            "Multiple instances of toxic are cumulative. For example, if a creature has toxic 2 and gains toxic 1 due to another effect, combat damage that creature deals to a player will cause that player to get 3 poison counters."
        )
    }
}
