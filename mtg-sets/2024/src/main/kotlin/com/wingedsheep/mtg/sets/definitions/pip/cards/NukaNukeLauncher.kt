package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nuka-Nuke Launcher
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature gets +3/+0 and has intimidate.
 * Whenever equipped creature attacks, until the end of defending player's next turn, that player
 * gets two rad counters whenever they cast a spell.
 * Equip {3}
 *
 * The attack trigger installs a delayed cast watcher scoped to the defending player (watchedTarget
 * on a cast trigger names the caster) that lasts through the cleanup of that player's next turn.
 */
val NukaNukeLauncher = card("Nuka-Nuke Launcher") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+0 and has intimidate. (It can't be blocked except by artifact " +
        "creatures and/or creatures that share a color with it.)\n" +
        "Whenever equipped creature attacks, until the end of defending player's next turn, that player " +
        "gets two rad counters whenever they cast a spell.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(3, 0)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.INTIMIDATE)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.anyPlayer.casts(),
            watchedTarget = EffectTarget.PlayerRef(Player.DefendingPlayer),
            expiry = DelayedTriggerExpiry.EndOfPlayersNextTurn(EffectTarget.PlayerRef(Player.DefendingPlayer)),
            effect = Effects.AddCounters(CounterType.RAD, 2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68d5835b-c206-4884-bdcf-38c7cdc703f4.jpg?1783912377"
    }
}
