package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Infesting Radroach — the recursion trigger functions from the graveyard (`triggerZone`), and its
 * "if this creature is in your graveyard" is an intervening-if (CR 603.4), so of several triggers
 * from one multi-card mill only the first can return it. Trigger detection reads the post-mill
 * state, so a Radroach milled alongside an opponent's nonland card still triggers (2024-03-08 ruling).
 */
val InfestingRadroach = card("Infesting Radroach") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Insect Mutant"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "This creature can't block.\n" +
        "Whenever this creature deals combat damage to a player, they get that many rad counters.\n" +
        "Whenever an opponent mills a nonland card, if this creature is in your graveyard, you may return it to your hand."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddDynamicCounters(
            CounterType.RAD,
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.mills(GameObjectFilter.Nonland)
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.SourceInZone(Zone.GRAVEYARD)
        optional = true
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Loïc Canavaggia"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b51b691-e226-44c9-b7cb-26f19c060597.jpg?1783912408"
        ruling(
            "2024-03-08",
            "If Infesting Radroach is milled at the same time an opponent mills a nonland card (for example, because of the activated ability of Raul, Trouble Shooter), Infesting Radroach's ability will trigger."
        )
    }
}
