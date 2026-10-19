package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jin-Gitaxias, Progress Tyrant — Kamigawa: Neon Dynasty #59.
 *
 * "Copy that spell" names the triggering spell, not a target, so the copy is made even when the
 * spell was countered before the trigger resolves: the copy is read from the spell's last-known
 * information as it last existed on the stack (CR 608.2h, `GameState.spellOrLastKnown`).
 */
val JinGitaxiasProgressTyrant = card("Jin-Gitaxias, Progress Tyrant") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    oracleText = "Whenever you cast an artifact, instant, or sorcery spell, copy that spell. You may choose new targets for the copy. This ability triggers only once each turn. (A copy of a permanent spell becomes a token.)\n" +
        "Whenever an opponent casts an artifact, instant, or sorcery spell, counter that spell. This ability triggers only once each turn."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Artifact or GameObjectFilter.InstantOrSorcery)
        oncePerTurn = true
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Artifact or GameObjectFilter.InstantOrSorcery)
        oncePerTurn = true
        effect = Effects.CounterTriggeringSpell()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "59"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c57b4876-5387-4f73-b8e2-8e7bdca8b0bc.jpg?1783923901"
        ruling("2022-02-18", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy created by Jin-Gitaxias's first ability will have the same mode or modes. You can't choose different ones.")
        ruling("2022-02-18", "If the spell that's copied had damage divided to targets as it was cast, the division can't be changed, although the targets receiving that damage still can. The same is true of spells that distribute counters to targets as it was cast.")
        ruling("2022-02-18", "You can't choose to pay any additional costs for the copy created by Jin-Gitaxias's first ability. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too.")
        ruling("2022-02-18", "The copies that Jin-Gitaxias's first ability creates are created on the stack, so they're not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2022-02-18", "A copy is created even if the spell that caused the ability to trigger has been countered by the time that ability resolves. The copy resolves before the original spell.")
        ruling("2022-02-18", "Jin-Gitaxias's last ability triggers once each turn, not once each turn per opponent.")
    }
}
