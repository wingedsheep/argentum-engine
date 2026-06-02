package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeLoss
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bloodletter of Aclazotz {1}{B}{B}{B}
 * Creature — Vampire Demon
 * 2/4
 *
 * Flying
 * If an opponent would lose life during your turn, they lose twice that much life
 * instead. (Damage causes loss of life.)
 *
 * Implementation note: the doubling is applied at the life-total reduction step in
 * `DamageUtils` (both for direct life loss and for damage that causes life loss per
 * CR 119.3). Lifelink and other damage-based effects continue to see the original
 * damage amount, matching the printed ruling.
 */
val BloodletterOfAclazotz = card("Bloodletter of Aclazotz") {
    manaCost = "{1}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Demon"
    power = 2
    toughness = 4
    oracleText = "Flying\nIf an opponent would lose life during your turn, they lose twice that much life instead. (Damage causes loss of life.)"

    keywords(Keyword.FLYING)

    replacementEffect(
        ModifyLifeLoss(
            multiplier = 2,
            restrictions = listOf(IsYourTurn),
            appliesTo = EventPattern.LifeLossEvent(player = Player.Opponent),
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "92"
        artist = "Antonio José Manzanedo"
        flavorText = "Its very presence rips old wounds open to bleed anew."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4f6027a-003a-4f9d-929a-0b6da1fa42c9.jpg?1699044094"

        ruling("2023-11-10", "Bloodletter of Aclazotz's last ability doesn't change the amount of damage dealt to opponents. For example, if a 1/1 creature with lifelink deals combat damage to an opponent on your turn, they would lose 2 life, but you'd still gain only 1 life.")
    }
}
