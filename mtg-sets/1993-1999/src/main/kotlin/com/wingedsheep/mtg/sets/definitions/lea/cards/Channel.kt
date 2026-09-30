package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PlayerActionTiming

val Channel = card("Channel") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, any time you could activate a mana ability, you may pay 1 life. If you do, add {C}."

    spell {
        effect = Effects.GrantPlayerAction(
            cost = Costs.pay.PayLife(1), effect = Effects.AddColorlessMana(1),
            timing = PlayerActionTiming.ManaAbility,
            actionDescription = "Channel: Pay 1 life to add {C}",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1862c47-71cc-45a3-8805-a5ddc62e55ea.jpg?1783948678"
        ruling("2017-11-17", "Once your life total is 0, you can't pay any more life, even if you've somehow not lost the game yet.")
    }
}
