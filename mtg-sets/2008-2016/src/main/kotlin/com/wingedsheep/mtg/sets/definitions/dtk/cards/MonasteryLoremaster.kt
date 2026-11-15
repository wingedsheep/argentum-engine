package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Monastery Loremaster
 * {3}{U}
 * Creature — Djinn Wizard
 * 3/2
 * Megamorph {5}{U}
 * When this creature is turned face up, return target noncreature, nonland card from your
 * graveyard to your hand.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The graveyard return is a separate
 * "turned face up" trigger, so it fires however the creature is turned face up.
 */
val MonasteryLoremaster = card("Monastery Loremaster") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn Wizard"
    power = 3
    toughness = 2
    oracleText = "Megamorph {5}{U} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, return target noncreature, nonland card from your " +
        "graveyard to your hand."

    // Megamorph {5}{U}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{5}{U}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val t = target(
            TargetFilter(
                baseFilter = (GameObjectFilter.Noncreature and GameObjectFilter.Nonland).ownedByYou(),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.Move(t, Zone.HAND)
        description = "When this creature is turned face up, return target noncreature, nonland card " +
            "from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19b9d8de-5746-4af0-9353-a19fa977df41.jpg?1783938606"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
