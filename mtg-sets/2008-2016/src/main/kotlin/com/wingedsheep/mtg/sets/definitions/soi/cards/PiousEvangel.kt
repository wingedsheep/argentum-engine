package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pious Evangel // Wayward Disciple (Shadows over Innistrad #34)
 * {2}{W}
 * Creature — Human Cleric 2/2 // Creature — Human Cleric 2/4
 *
 * Front — "Whenever this creature or another creature you control enters, you gain 1 life.
 *         {2}, {T}, Sacrifice another permanent: Transform this creature."
 * Back  — "Whenever this creature or another creature you control dies, target opponent loses 1 life
 *         and you gain 1 life."
 *
 * Both triggers read "this creature **or** another creature you control", so they use the ANY
 * binding of `Triggers.a(...)`: the Evangel's own arrival and the Disciple's own death fire them too,
 * once per creature when several enter or die at the same time (the rulings).
 *
 * The back face has no mana cost, so its black comes from a color indicator (CR 204).
 */

private val PiousEvangelFront = card("Pious Evangel") {
    manaCost = "{2}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or another creature you control enters, you gain 1 life.\n" +
        "{2}, {T}, Sacrifice another permanent: Transform this creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GainLife(1)
        description = "Whenever this creature or another creature you control enters, you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.SacrificeAnother(),
        )
        effect = Effects.Transform(EffectTarget.Self)
        description = "{2}, {T}, Sacrifice another permanent: Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Kev Walker"
        flavorText = "\"My faith guides my hand . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7ac2a5aa-71c3-482b-9431-5f0b228cf64d.jpg?1783937819"
        ruling(
            "2016-04-08",
            "If Pious Evangel enters the battlefield at the same time as another creature you control, " +
                "Pious Evangel's ability triggers once for the other creature as well as once for itself."
        )
    }
}

private val WaywardDisciple = card("Wayward Disciple") {
    manaCost = ""
    colorIdentity = "WB"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature or another creature you control dies, target opponent loses 1 life " +
        "and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(1, opponent) then Effects.GainLife(1)
        description = "Whenever this creature or another creature you control dies, target opponent loses " +
            "1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Kev Walker"
        flavorText = "\". . . and I will prove my devotion.\""
        imageUri = "https://cards.scryfall.io/normal/back/7/a/7ac2a5aa-71c3-482b-9431-5f0b228cf64d.jpg?1783937819"
        ruling(
            "2016-04-08",
            "If Wayward Disciple dies at the same time as another creature you control, Wayward " +
                "Disciple's ability triggers once for the other creature as well as once for itself."
        )
    }
}

val PiousEvangel: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = PiousEvangelFront,
    backFace = WaywardDisciple,
)
