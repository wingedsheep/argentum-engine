package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Locust God — Hour of Devastation #139 (canonical printing)
 * {4}{U}{R} · Legendary Creature — God · 4/4
 *
 * Flying
 * Whenever you draw a card, create a 1/1 blue and red Insect creature token with flying and haste.
 * {2}{U}{R}: Draw a card, then discard a card.
 * When The Locust God dies, return it to its owner's hand at the beginning of the next end step.
 *
 * `Triggers.you.draws()` fires once per card drawn. The dies trigger is The Scarab God's: a one-shot
 * at the next end step (any player's) that returns the card from the graveyard, and does nothing if
 * it has already left the graveyard (ruling).
 */
val TheLocustGod = card("The Locust God") {
    manaCost = "{4}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — God"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you draw a card, create a 1/1 blue and red Insect creature token with flying and haste.\n" +
        "{2}{U}{R}: Draw a card, then discard a card.\n" +
        "When The Locust God dies, return it to its owner's hand at the beginning of the next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE, Color.RED),
            creatureTypes = setOf("Insect"),
            keywords = setOf(Keyword.FLYING, Keyword.HASTE),
            imageUri = "https://cards.scryfall.io/normal/front/a/e/ae56d9e8-de05-456b-af32-b5992992ee15.jpg?1783936064"
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}{R}")
        effect = Patterns.Hand.loot()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.Move(EffectTarget.TriggeringEntity, Zone.HAND, fromZone = Zone.GRAVEYARD),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "139"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e0c1d0a-2651-4961-8b70-ff78093732ce.jpg?1783936012"
        ruling("2017-07-14", "If a spell or ability causes you to put cards into your hand without specifically using the word \"draw,\" The Locust God's first triggered ability won't trigger.")
        ruling("2017-07-14", "If this creature dies but leaves your graveyard before the next end step, it will remain in its new zone.")
        ruling("2017-07-14", "The \"next end step\" refers to the next end step that occurs, not the end step of the next turn. If this creature dies before a turn's end step (for example, during combat), it will be returned to its owner's hand at the beginning of that turn's end step.")
    }
}
