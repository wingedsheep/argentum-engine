package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Apex of Power — Core Set 2019 #129 (canonical printing)
 * {7}{R}{R}{R} · Sorcery
 *
 * Exile the top seven cards of your library. Until end of turn, you may cast spells from among them.
 * If this spell was cast from your hand, add ten mana of any one color.
 *
 * The top seven are exiled and given an end-of-turn cast permission — "cast spells", so
 * `nonLandOnly`: an exiled land can't be played and stays in exile (ruling). Normal timing still
 * applies and a card cast leaves exile. The mana is gated on [Conditions.WasCastFromHand], which a
 * copy fails (a copy wasn't cast), so a copy adds no mana (ruling).
 */
val ApexOfPower = card("Apex of Power") {
    manaCost = "{7}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile the top seven cards of your library. Until end of turn, you may cast spells from among them.\n" +
        "If this spell was cast from your hand, add ten mana of any one color."

    spell {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(7))
            exile(exiled)
            run(
                Effects.GrantMayPlayFromExile(
                    from = exiled,
                    expiry = MayPlayExpiry.EndOfTurn,
                    nonLandOnly = true,
                )
            )
        } then Effects.If(
            condition = Conditions.WasCastFromHand,
            then = Effects.AddAnyColorMana(10),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "129"
        artist = "Svetlin Velinov"
        flavorText = "\"As I desire, so it shall be.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c827bccf-38f9-4a7c-bd0e-038594a9f63b.jpg?1783934556"
        ruling("2018-07-13", "Apex of Power doesn't change when you can cast the exiled cards. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty.")
        ruling("2018-07-13", "Casting an exiled card causes it to leave exile. You can't cast it multiple times.")
        ruling("2018-07-13", "Any cards not cast, including land cards, remain in exile.")
        ruling("2018-07-13", "If an effect copies Apex of Power, the copy wasn't cast at all, so you won't add ten mana.")
    }
}
