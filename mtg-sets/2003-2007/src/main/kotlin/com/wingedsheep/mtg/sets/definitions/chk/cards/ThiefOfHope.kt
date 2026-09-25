package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thief of Hope
 * {2}{B}
 * Creature — Spirit
 * 2/2
 * Whenever you cast a Spirit or Arcane spell, target opponent loses 1 life and you gain 1 life.
 * Soulshift 2 (When this creature dies, you may return target Spirit card with mana value 2 or less
 * from your graveyard to your hand.)
 */
val ThiefOfHope = card("Thief of Hope") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a Spirit or Arcane spell, target opponent loses 1 life and you gain 1 life.\n" +
        "Soulshift 2 (When this creature dies, you may return target Spirit card with mana value 2 or less " +
        "from your graveyard to your hand.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withAnySubtype("Spirit", "Arcane"))
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(1, opponent) then Effects.GainLife(1)
        description = "Whenever you cast a Spirit or Arcane spell, target opponent loses 1 life and you gain 1 life."
    }

    soulshift(2)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a3029b8-01e2-4419-817e-318f23d6ce04.jpg?1783944306"
    }
}
