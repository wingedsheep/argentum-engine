package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Surveillance Monitor
 * {3}{U}
 * Creature — Vedalken Detective
 * 3/3
 *
 * When this creature enters, you may collect evidence 4.
 * Whenever you collect evidence, create a 1/1 colorless Thopter artifact creature token with flying.
 *
 * The **payoff** shape — and the card that proves the payment is genuinely unified. Its own enters
 * trigger collects evidence, which fires its own "whenever you collect evidence" trigger; but the
 * same payoff also fires for evidence collected as an *activated-ability* cost, a *cast* cost, or a
 * ward cost, because all four contexts route through one payment that emits one
 * `EvidenceCollectedEvent`.
 *
 * The enters clause is a bare "you may" with no rider, so it is [Effects.CollectEvidence] under an
 * optional gate rather than a reflexive trigger — there is no "when you do" to put on the stack.
 * Per CR 701.59b the prompt is skipped entirely when the graveyard can't reach 4.
 */
val SurveillanceMonitor = card("Surveillance Monitor") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Detective"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may collect evidence 4. (Exile cards with total " +
        "mana value 4 or greater from your graveyard.)\n" +
        "Whenever you collect evidence, create a 1/1 colorless Thopter artifact creature token " +
        "with flying."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(Effects.CollectEvidence(4))
        description = "When this creature enters, you may collect evidence 4."
    }

    triggeredAbility {
        trigger = Triggers.WheneverYouCollectEvidence
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            name = "Thopter",
        )
        description = "Whenever you collect evidence, create a 1/1 colorless Thopter artifact " +
            "creature token with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Scott Murphy"
        flavorText = "Thopters don't take breaks, and neither does he."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/703b874d-6739-4063-9891-e9c040dd9618.jpg"
    }
}
