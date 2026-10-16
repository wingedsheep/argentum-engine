package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Crimson Caravaneer — Fallout #56
 * {2}{R} · Creature — Human Scout · 1/2
 *
 * Double strike, trample. With double strike, connecting in both damage steps triggers twice.
 */
val CrimsonCaravaneer = card("Crimson Caravaneer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Scout"
    power = 1
    toughness = 2
    oracleText = "Double strike, trample\n" +
        "Whenever this creature deals combat damage to a player, create a Junk token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. " +
        "You may play that card this turn. Activate only as a sorcery.\")"

    keywords(Keyword.DOUBLE_STRIKE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateJunk()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3acc522b-6297-455e-8bb5-6187061496d0.jpg?1783912404"
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-03-08", "Some spells and abilities that create Junk tokens may require targets. If each target chosen is illegal as that spell or ability tries to resolve, it won't resolve and none of its effects happen. You won't create any Junk tokens.")
    }
}
