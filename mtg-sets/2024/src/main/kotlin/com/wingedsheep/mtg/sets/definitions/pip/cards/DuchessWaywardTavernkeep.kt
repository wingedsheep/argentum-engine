package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Duchess, Wayward Tavernkeep — Fallout #57
 * {3}{R} · Legendary Creature — Human Citizen · 4/3
 *
 * Hunters for Hire triggers once per creature you control that deals combat damage to a player,
 * and the quest counter goes on that creature (the triggering entity). The activated ability's
 * quest counter may come off any permanent you control, Duchess included.
 */
val DuchessWaywardTavernkeep = card("Duchess, Wayward Tavernkeep") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Citizen"
    power = 4
    toughness = 3
    oracleText = "Hunters for Hire — Whenever a creature you control deals combat damage to a player, put a quest counter on it.\n{1}, Remove a quest counter from a permanent you control: Create a Junk token. (It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. You may play that card this turn. Activate only as a sorcery.\")"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.TriggeringEntity)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.RemoveCounters(count = 1, counterType = CounterType.QUEST, filter = GameObjectFilter.Permanent),
        )
        effect = Effects.CreateJunk()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38c0a17f-787b-41ac-8375-d9532dce1f4f.jpg?1783912405"
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
