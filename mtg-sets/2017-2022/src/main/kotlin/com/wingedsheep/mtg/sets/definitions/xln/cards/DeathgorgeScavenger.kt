package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathgorge Scavenger — Ixalan #184 (canonical printing)
 * {2}{G} · Creature — Dinosaur · 3/2
 *
 * Whenever this creature enters or attacks, you may exile target card from a graveyard. If a
 * creature card is exiled this way, you gain 2 life. If a noncreature card is exiled this way,
 * this creature gets +1/+1 until end of turn.
 *
 * "enters or attacks" is two triggered abilities. The target is chosen when the trigger is put on
 * the stack; the "may" is asked at resolution and gates the whole exile-and-payoff, so declining
 * exiles nothing and grants neither bonus. Once the card is exiled, exactly one payoff applies:
 * [Conditions.TargetIsCreatureCard] reads the exiled card's printed type, and its `otherwise`
 * branch covers the noncreature case.
 */
val DeathgorgeScavenger = card("Deathgorge Scavenger") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature enters or attacks, you may exile target card from a graveyard. " +
        "If a creature card is exiled this way, you gain 2 life. If a noncreature card is exiled this way, " +
        "this creature gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.May(
            Effects.Exile(exiled) then
                Effects.If(
                    condition = Conditions.TargetIsCreatureCard(0),
                    then = Effects.GainLife(2),
                    otherwise = Effects.ModifyStats(1, 1, EffectTarget.Self),
                )
        )
        description = "Whenever this creature enters, you may exile target card from a graveyard. " +
            "If a creature card is exiled this way, you gain 2 life. If a noncreature card is exiled " +
            "this way, this creature gets +1/+1 until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.May(
            Effects.Exile(exiled) then
                Effects.If(
                    condition = Conditions.TargetIsCreatureCard(0),
                    then = Effects.GainLife(2),
                    otherwise = Effects.ModifyStats(1, 1, EffectTarget.Self),
                )
        )
        description = "Whenever this creature attacks, you may exile target card from a graveyard. " +
            "If a creature card is exiled this way, you gain 2 life. If a noncreature card is exiled " +
            "this way, this creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0e9d8a4-dfc2-4451-a15f-98592b817155.jpg?1783935730"
    }
}
