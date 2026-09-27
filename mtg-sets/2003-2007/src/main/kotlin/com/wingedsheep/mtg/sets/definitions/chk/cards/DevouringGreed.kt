package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Devouring Greed — Champions of Kamigawa #110
 * {2}{B}{B} · Sorcery — Arcane
 *
 * As an additional cost to cast this spell, you may sacrifice any number of Spirits.
 * Target player loses 2 life plus 2 life for each Spirit sacrificed this way. You gain that much life.
 *
 * The Spirits are chosen and sacrificed as the spell is cast; choosing none is a legal payment.
 */
val DevouringGreed = card("Devouring Greed") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery — Arcane"
    oracleText = "As an additional cost to cast this spell, you may sacrifice any number of Spirits.\n" +
        "Target player loses 2 life plus 2 life for each Spirit sacrificed this way. You gain that much life."

    additionalCost(Costs.additional.SacrificePermanents(GameObjectFilter.Permanent.withSubtype("Spirit")))

    spell {
        val player = target(Targets.Player)
        val amount = 2 + DynamicAmounts.permanentsSacrificedThisWay() * 2
        effect = Effects.LoseLife(amount, player) then Effects.GainLife(amount)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Vance Kovacs"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a72347d-91cd-45a8-a026-a655e5507322.jpg?1783944315"
    }
}
