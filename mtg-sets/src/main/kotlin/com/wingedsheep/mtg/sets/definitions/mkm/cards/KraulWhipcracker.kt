package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Kraul Whipcracker — Murders at Karlov Manor #213
 * {B}{G} · Creature — Insect Assassin · 3/2
 *
 * Reach
 * When this creature enters, destroy target token an opponent controls.
 *
 * "Token" is not a card type, so the target is `GameObjectFilter.Token.opponentControls()` —
 * any token permanent, Clue and Treasure and Plant included, not just creature tokens. The
 * target is mandatory: with no opposing token on the battlefield the trigger has no legal target
 * and is removed from the stack (CR 603.3d), and Kraul Whipcracker still enters as a 3/2.
 */
val KraulWhipcracker = card("Kraul Whipcracker") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Insect Assassin"
    oracleText = "Reach\nWhen this creature enters, destroy target token an opponent controls."
    power = 3
    toughness = 2

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "target token an opponent controls",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Token.opponentControls()))
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Filip Burburan"
        flavorText = "\"Er, sir, we could try to cite him for mishandling evidence, but I'm not sure he technically has hands.\"\n—Argyle, Agency junior detective"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c08ed44-2c13-4029-af2e-68585a76bb03.jpg?1783912845"
    }
}
