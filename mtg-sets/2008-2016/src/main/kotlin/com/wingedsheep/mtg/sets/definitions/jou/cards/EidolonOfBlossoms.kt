package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Eidolon of Blossoms
 * {2}{G}{G}
 * Enchantment Creature — Spirit
 * 2/2
 *
 * Constellation — Whenever this creature or another enchantment you control enters, draw a card.
 *
 * Constellation is an ability word with no rules meaning. The Eidolon is itself an enchantment, so
 * "this creature or another enchantment you control" is one unbound watcher over enchantments you
 * control (`Triggers.a`), which fires for the Eidolon's own entry too — the same shape as Oath of
 * the Ancient Wood's "this enchantment or another enchantment you control".
 */
val EidolonOfBlossoms = card("Eidolon of Blossoms") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Effects.DrawCards(1)
        description = "Constellation — Whenever this creature or another enchantment you control enters, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Min Yum"
        flavorText = "The emotional echoes of dryad gatherings attract lost souls."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dbb2112-85b9-49cf-9ed5-9273829c34f1.jpg?1783939415"
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
