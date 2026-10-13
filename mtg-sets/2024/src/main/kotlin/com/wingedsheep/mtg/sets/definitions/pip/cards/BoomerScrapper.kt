package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boomer Scrapper — Fallout #95.
 *
 * "A token you control" is the bare [GameObjectFilter.Token] — any token, not just creature tokens
 * (so the Junk it makes counts when it's sacrificed for its own ability) — and the trigger is a
 * destination-less `leaves()`, so sacrifice, exile and bounce all fire it.
 */
val BoomerScrapper = card("Boomer Scrapper") {
    manaCost = "{1}{B}{R}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature enters or attacks, you lose 1 life and create a Junk token. (It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. You may play that card this turn. Activate only as a sorcery.\")\nWhenever a token you control leaves the battlefield, put a +1/+1 counter on this creature."
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.LoseLife(1, EffectTarget.Controller) then Effects.CreateJunk()
    }
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.LoseLife(1, EffectTarget.Controller) then Effects.CreateJunk()
    }
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Token.youControl()).leaves()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "95"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/193d0316-9b94-4ccb-9103-509e6a0963a1.jpg?1783912394"
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
