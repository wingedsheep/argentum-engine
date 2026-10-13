package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

val RoseCutthroatRaider = card("Rose, Cutthroat Raider") {
    manaCost = "{2}{R}{R}"
    typeLine = "Legendary Artifact Creature — Robot"
    power = 3
    toughness = 2
    oracleText = "First strike\nRaid — At end of combat on your turn, if you attacked this turn, create a Junk token for each opponent you attacked. (It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. You may play that card this turn. Activate only as a sorcery.\")\nWhenever you sacrifice a Junk, add {R}."
    keywords(Keyword.FIRST_STRIKE)
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END_COMBAT)
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.CreateJunk(DynamicAmounts.opponentsAttackedThisTurn())
    }
    triggeredAbility {
        trigger = Triggers.you.sacrifices(GameObjectFilter.Any.withSubtype("Junk"))
        effect = Effects.AddMana(Color.RED)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f803a510-99fe-467a-be3b-9301497ec7ec.jpg?1783912401"
        ruling("2024-11-08", "Raid abilities care only that you attacked with a creature. It doesn't matter how many creatures you attacked with or which player, planeswalker, or battle those creatures attacked.")
        ruling("2024-11-08", "Raid abilities evaluate the entire turn to see if you attacked with a creature. That creature doesn't have to still be on the battlefield. Similarly, the player, planeswalker, or battle it attacked doesn't have to still be in the game or on the battlefield.")
        ruling("2024-03-08", "Rose's last ability triggers when you sacrifice a Junk for any reason, not just to activate a Junk's activated ability.")
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
