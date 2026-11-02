package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Suncleanser
 * {1}{W}
 * Creature — Human Cleric
 * 1/4
 * When this creature enters, choose one —
 * • Remove all counters from target creature. It can't have counters put on it for as long as this
 *   creature remains on the battlefield.
 * • Target opponent loses all counters. That player can't get counters for as long as this creature
 *   remains on the battlefield.
 *
 * The creature mode composes a `CANT_RECEIVE_COUNTERS` grant with a source-keyed duration; the
 * player mode is the same lock on a player ([Effects.LockPlayerCounters]), since players aren't
 * projected. Either lock ends for good once Suncleanser leaves the battlefield.
 */
val Suncleanser = card("Suncleanser") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 4
    oracleText = "When this creature enters, choose one —\n" +
        "• Remove all counters from target creature. It can't have counters put on it for as long as this creature remains on the battlefield.\n" +
        "• Target opponent loses all counters. That player can't get counters for as long as this creature remains on the battlefield."

    val whileHere = Duration.WhileSourceOnBattlefield("Suncleanser")

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Remove all counters from target creature. It can't have counters put on it for as long as Suncleanser remains on the battlefield") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.RemoveAllCounters(creature) then
                    Effects.GrantKeyword(AbilityFlag.CANT_RECEIVE_COUNTERS, creature, whileHere)
            },
            mode("Target opponent loses all counters. That player can't get counters for as long as Suncleanser remains on the battlefield") {
                val opponent = target(Targets.Opponent)
                effect = Effects.RemoveAllCounters(opponent) then
                    Effects.LockPlayerCounters(opponent, whileHere)
            },
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3644df41-b690-4581-ac7d-c85cec75411f.jpg?1783934596"
        ruling("2018-07-13", "If the cost of an ability or an additional cost of a spell requires putting counters on a creature or player affected by Suncleanser, that cost can't be paid. If a resolving spell or ability says that a player may give counters to that creature or player, that player can't choose to do so.")
        ruling("2018-07-13", "If a replacement effect allows a player to modify or replace an event by putting counters on a creature or player affected by Suncleanser, that player may apply that replacement effect, but they won't get counters. If the original event is entirely replaced (such as by applying Soul-Scar Mage's replacement effect), the original event won't happen.")
        ruling("2018-07-13", "Damage from a source with wither has no effect on an affected creature. No -1/-1 counters are put on them, and no damage is marked on them. The damage is still dealt for purposes of effects that care about damage, such as lifelink.")
        ruling("2018-07-13", "Damage from a source with infect has no effect on an affected creature or player. No -1/-1 counters are put on creatures, and no damage is marked on them. Players don't get poison counters and they don't lose life. The damage is still dealt for purposes of effects that care about damage, such as lifelink.")
        ruling("2018-07-13", "While resolving a cumulative upkeep trigger of an affected creature, you'll fail to put a counter on that permanent, then you may pay for the age counters already on it. If it has no age counters on it, you may pay {0}.")
    }
}
