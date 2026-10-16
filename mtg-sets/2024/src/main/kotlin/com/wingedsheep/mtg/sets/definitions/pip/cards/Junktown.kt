package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Junktown — Fallout #150
 * Land · Rare
 *
 * {T}: Add {C}.
 * {4}{R}, {T}, Sacrifice this land: Create three Junk tokens.
 *
 * The land's own ability has no timing restriction; only the Junk tokens' abilities are
 * sorcery-speed.
 */
val Junktown = card("Junktown") {
    colorIdentity = "R"
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{4}{R}, {T}, Sacrifice this land: Create three Junk tokens. (They're artifacts with " +
        "\"{T}, Sacrifice this token: Exile the top card of your library. You may play that card this turn. " +
        "Activate only as a sorcery.\")"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{R}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateJunk(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "150"
        artist = "Calder Moore"
        flavorText = "It's not much, but it's home."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e24c687-f070-469d-a09d-acf4ac6ed374.jpg?1783912373"
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-03-08", "Some spells and abilities that create Junk tokens may require targets. If each target chosen is illegal as that spell or ability tries to resolve, it won't resolve and none of its effects happen. You won't create any Junk tokens.")
    }
}
