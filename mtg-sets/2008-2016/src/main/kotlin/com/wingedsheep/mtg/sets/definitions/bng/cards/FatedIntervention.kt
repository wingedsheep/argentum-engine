package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fated Intervention — Born of the Gods #121 (canonical / earliest real printing)
 * {2}{G}{G}{G} · Instant
 *
 * Create two 3/3 green Centaur enchantment creature tokens. If it's your turn, scry 2.
 *
 * Two [Effects.CreateToken] Centaurs (Pheres-Band Raiders' token), then [Effects.Scry] gated on
 * [Conditions.IsYourTurn] — the Fated cycle's shared tail, checked at resolution.
 */
val FatedIntervention = card("Fated Intervention") {
    manaCost = "{2}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Create two 3/3 green Centaur enchantment creature tokens. If it's your turn, scry 2. " +
        "(Look at the top two cards of your library, then put any number of them on the bottom of " +
        "your library and the rest on top in any order.)"

    spell {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Centaur"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/9/8/985be507-6125-4db2-b99f-8b61149ffeeb.jpg?1783939515",
            enchantmentToken = true,
        ) then Effects.If(Conditions.IsYourTurn, Effects.Scry(2))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/622e1afc-0d06-4c01-abef-3b3e93bd21c3.jpg?1783939536"
    }
}
