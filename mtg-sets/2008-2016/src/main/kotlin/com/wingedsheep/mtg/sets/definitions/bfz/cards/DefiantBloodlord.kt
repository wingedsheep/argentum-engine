package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Defiant Bloodlord — Battle for Zendikar #107
 * {5}{B}{B} · Creature — Vampire · 4/5
 *
 * Flying
 * Whenever you gain life, target opponent loses that much life.
 *
 * "That much" is the amount gained by the triggering life-gain event
 * ([DynamicAmounts.triggerLifeGained]) — the Sanguine Bond shape. Each life-gain event
 * triggers separately.
 */
val DefiantBloodlord = card("Defiant Bloodlord") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever you gain life, target opponent loses that much life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        val opponent = target(Targets.Opponent)
        trigger = Triggers.you.gainsLife()
        effect = Effects.LoseLife(DynamicAmounts.triggerLifeGained(), opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Craig J Spearing"
        flavorText = "\"Despite their bravado, mortals are fragile. If Zendikar is to endure, its lords must save it.\"\n—Drana, Kalastria bloodchief"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4b5dd64-9e58-4f22-9d86-7a518d976037.jpg?1783938202"
        ruling(
            "2015-08-25",
            "The ability triggers just once for each life-gaining event, whether it's 1 life from Drana's Emissary or 7 life from Nissa's Renewal."
        )
        ruling(
            "2015-08-25",
            "A creature with lifelink dealing combat damage is a single life-gaining event. For example, if two creatures you control " +
                "with lifelink deal combat damage at the same time, the ability will trigger twice. However, if a single creature with " +
                "lifelink deals combat damage to multiple creatures, players, and/or planeswalkers at the same time (perhaps because it " +
                "has trample or was blocked by more than one creature), the ability will trigger only once."
        )
        ruling(
            "2015-08-25",
            "In a Two-Headed Giant game, life gained by your teammate won't cause the ability to trigger, even though it causes your team's life total to increase."
        )
    }
}
