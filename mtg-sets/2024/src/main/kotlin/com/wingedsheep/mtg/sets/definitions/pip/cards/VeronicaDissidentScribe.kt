package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Veronica, Dissident Scribe — Fallout #70.
 *
 * The Junk trigger is "for the first time each turn" turn history, not `oncePerTurn`: a nonland
 * discard earlier in the turn closes it even if Veronica wasn't on the battlefield then, and a land
 * discard doesn't.
 */
val VeronicaDissidentScribe = card("Veronica, Dissident Scribe") {
    manaCost = "{2}{R}"
    typeLine = "Legendary Creature — Human Artificer Rogue"
    power = 3
    toughness = 3
    oracleText = "Menace\nWhenever Veronica attacks, you may discard a card. If you do, draw a card.\nWhenever you discard one or more nonland cards for the first time each turn, create a Junk token. (It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. You may play that card this turn. Activate only as a sorcery.\")"
    keywords(Keyword.MENACE)
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(effect = Effects.IfYouDo(action = Patterns.Hand.discardCards(1), then = Effects.DrawCards(1)))
    }
    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Nonland, batch = true, firstTimeEachTurn = true)
        effect = Effects.CreateJunk()
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Pascal Quidault"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2325aa29-7050-42a5-a0b4-c162db2192c9.jpg?1783912399"
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-03-08", "Some spells and abilities that create Junk tokens may require targets. If each target chosen is illegal as that spell or ability tries to resolve, it won't resolve and none of its effects happen. You won't create any Junk tokens.")
    }
}
