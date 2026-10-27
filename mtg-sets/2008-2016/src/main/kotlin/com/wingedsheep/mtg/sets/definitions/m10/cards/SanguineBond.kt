package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sanguine Bond — Magic 2010 #111
 * {3}{B}{B} · Enchantment
 *
 * Whenever you gain life, target opponent loses that much life.
 *
 * "That much" is the amount gained by the triggering life-gain event
 * ([DynamicAmounts.triggerLifeGained]) — the Vito, Thorn of the Dusk Rose shape. Each life-gain
 * event triggers separately.
 */
val SanguineBond = card("Sanguine Bond") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever you gain life, target opponent loses that much life."

    triggeredAbility {
        val opponent = target(Targets.Opponent)
        trigger = Triggers.you.gainsLife()
        effect = Effects.LoseLife(DynamicAmounts.triggerLifeGained(), opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Jaime Jones"
        flavorText = "\"Blood is constant. Every drop I drink, someone must bleed.\"\n—Vradeen, vampire nocturnus"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/445d6a68-ed53-4c96-973a-c29282514f41.jpg?1783942379"
        ruling("2009-10-01", "Sanguine Bond's ability triggers just once for each life-gaining event, whether it's 1 life from Soul Warden or 8 life from Tendrils of Corruption.")
        ruling(
            "2021-03-19",
            "If you gain an amount of life \"for each\" of something, that life is gained as one event and Sanguine Bond's ability triggers only once."
        )
        ruling(
            "2021-03-19",
            "If an ability triggers whenever an opponent loses life and causes you to gain life, such as the ability of Exquisite Blood, " +
                "this will loop until either you win the game or a player takes an action to break the loop. If neither of these happen, the game ends in a draw."
        )
        ruling(
            "2023-09-01",
            "Each creature with lifelink dealing combat damage causes a separate life-gaining event. For example, if two creatures you " +
                "control with lifelink deal combat damage at the same time, Sanguine Bond's ability will trigger twice and you may choose a " +
                "different opponent for each trigger. However, if a single creature you control with lifelink deals combat damage to " +
                "multiple creatures, players, planeswalkers, and/or battles at the same time (perhaps because it has trample or was blocked " +
                "by more than one creature), the ability will trigger only once."
        )
        ruling(
            "2024-04-12",
            "In a Two-Headed Giant game, life gained by your teammate won't cause Sanguine Bond's ability to trigger, even though it caused your team's life total to increase."
        )
    }
}
