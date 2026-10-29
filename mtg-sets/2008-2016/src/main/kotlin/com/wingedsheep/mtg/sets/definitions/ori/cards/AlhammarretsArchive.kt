package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawStepFirstCard
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeGain
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Alhammarret's Archive
 * {5}
 * Legendary Artifact
 * If you would gain life, you gain twice that much life instead.
 * If you would draw a card except the first one you draw in each of your draw steps, draw two cards
 * instead.
 *
 * Phial of Galadriel's two replacements without its gates: `ModifyLifeGain(×2)` on your life gain,
 * and `ReplaceDrawWith(draw 2)` on your per-card draws with Chains of Mephistopheles'
 * `DrawStepFirstCard.EXCLUDED`, which exempts exactly the first card you draw in each of your own
 * draw steps.
 */
val AlhammarretsArchive = card("Alhammarret's Archive") {
    manaCost = "{5}"
    typeLine = "Legendary Artifact"
    oracleText = "If you would gain life, you gain twice that much life instead.\n" +
        "If you would draw a card except the first one you draw in each of your draw steps, draw two cards instead."

    replacementEffect(
        ModifyLifeGain(
            multiplier = 2,
            appliesTo = EventPattern.LifeGainEvent(player = Player.You),
        )
    )

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.DrawCards(2),
            appliesTo = EventPattern.DrawEvent(
                player = Player.You,
                firstInDrawStep = DrawStepFirstCard.EXCLUDED,
            ),
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "221"
        artist = "Richard Wright"
        flavorText = "It contains the totality of Alhammarret's notes, theories, and ponderings."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70afb856-6697-411b-b1aa-e9290a0bfd68.jpg?1783938312"
        ruling("2015-06-22", "If an effect would set your life total to a specific number that's higher than your current life total, that effect would cause you to gain life equal to the difference. Alhammarret's Archive will then double the amount of life that effect would cause you to gain. For example, if you have 3 life and an effect says that your life total \"becomes 10,\" your life total will actually become 17.")
        ruling("2015-06-22", "If two or more replacement effects would apply to a card-drawing event, the player drawing the card chooses the order in which to apply them.")
        ruling("2015-06-22", "Similarly, the effects of the last abilities of multiple Archives are cumulative. If you control two, you'll draw four times the number of cards, and so on.")
    }
}
