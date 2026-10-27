package com.wingedsheep.mtg.sets.definitions.lcc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clavileño, First of the Blessed
 * {1}{W}{B}
 * Legendary Creature — Vampire Cleric
 * 2/2
 * Whenever you attack, target attacking Vampire that isn't a Demon becomes a Demon in addition to
 * its other types. It gains "When this creature dies, draw a card and create a tapped 4/3 white
 * and black Vampire Demon creature token with flying."
 *
 * Both the added Demon type and the granted dies trigger have no duration, so they last for as long
 * as the creature stays on the battlefield. The granted trigger belongs to the Vampire, so its
 * controller draws and gets the token.
 */
val ClavilenoFirstOfTheBlessed = card("Clavileño, First of the Blessed") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Vampire Cleric"
    oracleText = "Whenever you attack, target attacking Vampire that isn't a Demon becomes a Demon in " +
        "addition to its other types. It gains \"When this creature dies, draw a card and create a " +
        "tapped 4/3 white and black Vampire Demon creature token with flying.\""
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val vampire = target(
            TargetFilter(
                GameObjectFilter.Creature.attacking()
                    .withSubtype(Subtype.VAMPIRE)
                    .notSubtype(Subtype.DEMON)
            )
        )
        effect = Effects.AddCreatureType("Demon", vampire, Duration.Permanent) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.DrawCards(1) then Effects.CreateToken(
                        power = 4,
                        toughness = 3,
                        colors = setOf(Color.WHITE, Color.BLACK),
                        creatureTypes = setOf("Vampire", "Demon"),
                        keywords = setOf(Keyword.FLYING),
                        tapped = true,
                        imageUri = "https://cards.scryfall.io/normal/front/3/0/3005eb0a-5c96-4a07-a6b9-a907d1095cdf.jpg?1783913605",
                    )
                ),
                target = vampire,
                duration = Duration.Permanent
            )
        description = "Whenever you attack, target attacking Vampire that isn't a Demon becomes a Demon " +
            "in addition to its other types. It gains \"When this creature dies, draw a card and " +
            "create a tapped 4/3 white and black Vampire Demon creature token with flying.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f55b648-b1ec-407a-a0c7-7647755793d1.jpg?1783913936"
    }
}
