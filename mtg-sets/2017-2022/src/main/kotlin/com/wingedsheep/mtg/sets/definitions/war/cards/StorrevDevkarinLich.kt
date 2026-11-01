package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Storrev, Devkarin Lich — War of the Spark #219
 * {1}{B}{B}{G} · Legendary Creature — Zombie Elf Wizard · Rare · 5/4
 *
 * Trample
 * Whenever Storrev deals combat damage to a player or planeswalker, return to your hand target
 * creature or planeswalker card in your graveyard that wasn't put there this combat.
 *
 * "Wasn't put there this combat" is `notPutIntoGraveyardThisCombat()`: a card that reached the
 * graveyard from any zone during the current combat phase — including Storrev itself, dying to
 * simultaneous combat damage — can't be chosen.
 */
val StorrevDevkarinLich = card("Storrev, Devkarin Lich") {
    manaCost = "{1}{B}{B}{G}"
    typeLine = "Legendary Creature — Zombie Elf Wizard"
    power = 5
    toughness = 4
    oracleText = "Trample\n" +
        "Whenever Storrev deals combat damage to a player or planeswalker, return to your hand " +
        "target creature or planeswalker card in your graveyard that wasn't put there this combat."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        val returned = target(
            TargetFilter(
                GameObjectFilter.CreatureOrPlaneswalker.ownedByYou().notPutIntoGraveyardThisCombat(),
                zone = Zone.GRAVEYARD
            )
        )
        effect = Effects.ReturnToHand(returned)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Igor Kieryluk"
        flavorText = "In times of war, only death flourishes."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59c906bd-f443-4f4b-b40b-9218e71f1196.jpg?1783933386"

        ruling("2019-05-03", "If Storrev dies due to combat damage dealt at the same time that it deals combat damage to a player or planeswalker, its ability still triggers.")
        ruling("2019-05-03", "Storrev's triggered ability can't target any card that was put into your graveyard during this combat phase. This includes creatures and planeswalkers that died from combat damage, creatures and planeswalkers that died for any other reason, and cards that were put there from any other zone.")
    }
}
