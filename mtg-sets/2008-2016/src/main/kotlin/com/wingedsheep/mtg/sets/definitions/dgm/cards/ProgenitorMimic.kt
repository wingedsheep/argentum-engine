package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Progenitor Mimic (Dragon's Maze #92 — the card's earliest printing)
 * {4}{G}{U}
 * Creature — Shapeshifter 0/0
 *
 * You may have this creature enter as a copy of any creature on the battlefield, except it has
 * "At the beginning of your upkeep, if this creature isn't a token, create a token that's a copy
 * of this creature."
 *
 * Implementation: an optional [EntersAsCopy] whose exception adds the upkeep trigger through
 * [CopyExceptions.addedTriggeredAbilities], so the trigger is a copiable value (CR 707.9a): each
 * token copy — and any later clone of the Mimic — carries it too. The "isn't a token" clause is a
 * genuine intervening-if, so the tokens' own instances never trigger, and a second Mimic copying
 * the first ends up with two instances.
 */
val ProgenitorMimic = card("Progenitor Mimic") {
    manaCost = "{4}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield, " +
        "except it has \"At the beginning of your upkeep, if this creature isn't a token, create a " +
        "token that's a copy of this creature.\""

    val upkeepCopy = grantedTriggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.SourceMatches(GameObjectFilter.Any.nontoken())
        effect = Effects.CreateTokenCopyOfSelf()
        description = "At the beginning of your upkeep, if this creature isn't a token, create a " +
            "token that's a copy of this creature."
    }
    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature,
            exceptions = CopyExceptions(addedTriggeredAbilities = listOf(upkeepCopy)),
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "92"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ad76314-b5d5-4353-86aa-e899e0d757a5.jpg"
        ruling(
            "2013-04-15",
            "Progenitor Mimic copies exactly what was printed on the original creature and nothing " +
                "more (unless that creature is copying something else or is a token). It doesn't copy " +
                "whether that creature is tapped or untapped, whether it has any counters on it or " +
                "Auras attached to it, or any non-copy effects that have changed its power, " +
                "toughness, types, color, or so on."
        )
        ruling("2013-04-15", "The original Progenitor Mimic will create tokens, but those token copies will not.")
        ruling(
            "2013-04-15",
            "If you choose to have Progenitor Mimic enter the battlefield as a copy of a creature, " +
                "the triggered ability it gains will become part of its copiable values. For example, " +
                "suppose Progenitor Mimic enters the battlefield as a copy of Runeclaw Bear, a 2/2 " +
                "green Bear creature with mana cost {1}{G}. The resulting object is a 2/2 green Bear " +
                "creature named Runeclaw Bear with mana cost {1}{G} and with \"At the beginning of " +
                "your upkeep, if this creature isn't a token, put a token onto the battlefield that's " +
                "a copy of this creature.\" If another Progenitor Mimic enters the battlefield as a " +
                "copy of that creature, it will be a Runeclaw Bear with two instances of the " +
                "triggered ability."
        )
        ruling(
            "2013-04-15",
            "If the chosen creature is a token, Progenitor Mimic copies the original characteristics " +
                "of that token as stated by the effect that put it onto the battlefield. Copying a " +
                "token doesn't make Progenitor Mimic become a token."
        )
        ruling(
            "2013-04-15",
            "Any enters-the-battlefield abilities of the copied creature will trigger when " +
                "Progenitor Mimic enters the battlefield. Any \"as [this creature] enters the " +
                "battlefield\" or \"[this creature] enters the battlefield with\" abilities of the " +
                "chosen creature will also work."
        )
        ruling(
            "2013-04-15",
            "If the chosen creature has {X} in its mana cost (such as Protean Hydra), X is " +
                "considered to be zero."
        )
        ruling(
            "2013-04-15",
            "If Progenitor Mimic somehow enters the battlefield at the same time as another " +
                "creature, it can't become a copy of that creature. You may choose only a creature " +
                "that's already on the battlefield."
        )
        ruling(
            "2013-04-15",
            "You can choose not to copy anything. In that case, Progenitor Mimic enters the " +
                "battlefield as a 0/0 Shapeshifter creature and is probably put into the graveyard " +
                "immediately."
        )
    }
}
