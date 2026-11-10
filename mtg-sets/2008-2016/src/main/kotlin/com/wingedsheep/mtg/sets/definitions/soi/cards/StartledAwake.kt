package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Startled Awake // Persistent Nightmare — Shadows over Innistrad #88
 * {2}{U}{U} · Sorcery // Creature — Nightmare 1/1
 *
 * Front — Startled Awake:
 *   Target opponent mills thirteen cards.
 *   {3}{U}{U}: Put this card from your graveyard onto the battlefield transformed. Activate only
 *   as a sorcery.
 *
 * Back — Persistent Nightmare:
 *   Skulk
 *   When this creature deals combat damage to a player, return it to its owner's hand.
 *
 * The graveyard ability is [Effects.ReturnSelfFromGraveyardTransformed] on an
 * `activateFromZone = Zone.GRAVEYARD` ability (Garland, Knight of Cornelia's shape). Returned to
 * hand, the card is Startled Awake again (off the battlefield a TDFC has only its front face), so
 * it can be cast, milled into the graveyard, and brought back — the loop the card is built for.
 */
private val PersistentNightmare = card("Persistent Nightmare") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Nightmare"
    power = 1
    toughness = 1
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "When this creature deals combat damage to a player, return it to its owner's hand."

    keywords(Keyword.SKULK)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ReturnToHand(EffectTarget.Self)
        description = "When Persistent Nightmare deals combat damage to a player, return it to its owner's hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "88"
        artist = "Sean Sevestre"
        flavorText = "\"It seemed so real!\""
        imageUri = "https://cards.scryfall.io/normal/back/e/6/e630fc56-a96f-4abf-be8d-f0f40d5a9edf.jpg?1783937793"
    }
}

private val StartledAwakeFront = card("Startled Awake") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target opponent mills thirteen cards.\n" +
        "{3}{U}{U}: Put this card from your graveyard onto the battlefield transformed. " +
        "Activate only as a sorcery."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Patterns.Library.mill(13, opponent)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}{U}")
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.ReturnSelfFromGraveyardTransformed()
        description = "Put Startled Awake from your graveyard onto the battlefield transformed."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "88"
        artist = "Sean Sevestre"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e630fc56-a96f-4abf-be8d-f0f40d5a9edf.jpg?1783937793"
        ruling(
            "2016-04-08",
            "A sorcery can't be put onto the battlefield and a permanent can't transform into a " +
                "sorcery. If an effect exiles Persistent Nightmare and then instructs you to return it " +
                "to the battlefield, it remains face up in exile (unless that effect instructs you to " +
                "put it onto the battlefield transformed, in which case it returns as Persistent " +
                "Nightmare). If an effect instructs you to transform Persistent Nightmare, the " +
                "instruction is ignored."
        )
    }
}

/**
 * A transforming double-faced card with a sorcery front: the front is a non-permanent face, so the
 * card is assembled with a raw `copy(backFace = …)` rather than the permanent-front DFC factories
 * (Esper Origins' shape).
 */
val StartledAwake: CardDefinition = StartledAwakeFront.copy(backFace = PersistentNightmare)
