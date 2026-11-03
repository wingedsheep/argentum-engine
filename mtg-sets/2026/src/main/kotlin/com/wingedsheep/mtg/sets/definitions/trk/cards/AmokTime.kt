package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Amok Time — Star Trek #183
 * {2}{G} · Enchantment — Saga
 *
 * I — Target creature you control fights up to one target creature an opponent controls.
 * II — You gain 3 life.
 * III — Return target creature card from your graveyard to your hand.
 *
 * Chapter I with no second target chosen fights nobody (Arni Slays the Troll's shape).
 */
val AmokTime = card("Amok Time") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Target creature you control fights up to one target creature an opponent controls.\n" +
        "II — You gain 3 life.\n" +
        "III — Return target creature card from your graveyard to your hand."

    sagaChapter(1) {
        val mine = target(TargetFilter.CreatureYouControl)
        val theirs = target(TargetFilter.CreatureOpponentControls, optional = true)
        effect = Effects.Fight(mine, theirs)
    }

    sagaChapter(2) {
        effect = Effects.GainLife(3)
    }

    sagaChapter(3) {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creatureCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Jason Raish"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d31447b6-484f-4dda-b6af-ab068a84f896.jpg?1787205814"
    }
}
