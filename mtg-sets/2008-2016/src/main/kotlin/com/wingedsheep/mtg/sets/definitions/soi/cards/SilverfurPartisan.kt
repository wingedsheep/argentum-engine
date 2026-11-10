package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Silverfur Partisan
 * {2}{G}
 * Creature — Wolf Warrior
 * 2/2
 * Trample
 * Whenever a Wolf or Werewolf you control becomes the target of an instant or sorcery spell,
 * create a 2/2 green Wolf creature token.
 *
 * The targeting spell may be controlled by any player (no "you control" on the spell), and only
 * spells count — an ability targeting the Wolf doesn't. "A Wolf or Werewolf" is any permanent with
 * either subtype, the Partisan itself included.
 */
val SilverfurPartisan = card("Silverfur Partisan") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf Warrior"
    power = 2
    toughness = 2
    oracleText = "Trample\n" +
        "Whenever a Wolf or Werewolf you control becomes the target of an instant or sorcery " +
        "spell, create a 2/2 green Wolf creature token."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Permanent.withAnySubtype("Wolf", "Werewolf").youControl()
        ).becomesTarget(of = GameObjectFilter.InstantOrSorcery, spellsOnly = true)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Izzy"
        flavorText = "Silverfurs are the last remaining wolfir."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a20f5392-5cde-4326-a322-7463ec4b0515.jpg?1783937721"
    }
}
