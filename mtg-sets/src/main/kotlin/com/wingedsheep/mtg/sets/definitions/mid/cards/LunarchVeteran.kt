package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Lunarch Veteran // Luminous Phantom (Innistrad: Midnight Hunt #27 — the card's earliest
 * printing; also reprinted in Innistrad Remastered)
 * {W} · Creature — Human Cleric 1/1 // Creature — Spirit Cleric 1/1
 *
 * Front — Lunarch Veteran ({W}, Creature — Human Cleric, 1/1)
 *   Whenever another creature you control enters, you gain 1 life.
 *   Disturb {1}{W}
 *
 * Back — Luminous Phantom (Creature — Spirit Cleric, 1/1, white color indicator)
 *   Flying
 *   Whenever another creature you control leaves the battlefield, you gain 1 life.
 *   If Luminous Phantom would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation:
 *  - Disturb (CR 702.146) is the [disturb] keyword on the front face. The engine's
 *    cast-from-graveyard enumerator offers the back face for the disturb cost, and the spell goes
 *    on the stack back face up (CR 712.8c), so the Phantom's triggers and flying are what the
 *    resolving permanent has.
 *  - The two life triggers are mirror images: [Triggers.OtherCreatureEnters] on the front and
 *    `leavesBattlefield(Creature.youControl(), binding = OTHER)` on the back. Both are OTHER-bound,
 *    so neither face's own arrival or departure feeds itself.
 *  - "Would be put into a graveyard from anywhere, exile it instead" is the reusable
 *    [RedirectZoneChange] with `selfOnly = true`, which the engine carries on the card entity so it
 *    functions in every zone (CR 614.12) — including the stack, so a countered disturb spell is
 *    exiled rather than returning to the graveyard to be disturbed again.
 */
private val LunarchVeteranFront = card("Lunarch Veteran") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Whenever another creature you control enters, you gain 1 life.\n" +
        "Disturb {1}{W} (You may cast this card from your graveyard transformed for its disturb cost.)"

    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        effect = Effects.GainLife(1)
        description = "Whenever another creature you control enters, you gain 1 life."
    }

    disturb("{1}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Igor Kieryluk"
        flavorText = "\"Even in our darkest times, Avacyn's light still guides us.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2704743-2e23-40b9-a367-c73d2db45afc.jpg?1783925662"
        ruling(
            "2021-09-24",
            "When you cast a spell using a card's disturb ability, the card is put onto the stack " +
                "with its back face up. The resulting spell has all the characteristics of that face."
        )
        ruling(
            "2021-09-24",
            "The mana value of a spell cast using disturb is determined by the mana cost on the " +
                "front face of the card, no matter what the total cost to cast the spell was."
        )
        ruling(
            "2021-09-24",
            "The back face of each card with disturb has an ability that instructs its controller " +
                "to exile it if it would be put into a graveyard from anywhere. This includes going " +
                "to the graveyard from the stack, so if the spell is countered after you cast it " +
                "using the disturb ability, it will be put into exile."
        )
    }
}

private val LuminousPhantom = card("Luminous Phantom") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Creature — Spirit Cleric"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever another creature you control leaves the battlefield, you gain 1 life.\n" +
        "If Luminous Phantom would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.OTHER,
        )
        effect = Effects.GainLife(1)
        description = "Whenever another creature you control leaves the battlefield, you gain 1 life."
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/back/d/2/d2704743-2e23-40b9-a367-c73d2db45afc.jpg?1783925662"
    }
}

val LunarchVeteran: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = LunarchVeteranFront,
    backFace = LuminousPhantom,
)
