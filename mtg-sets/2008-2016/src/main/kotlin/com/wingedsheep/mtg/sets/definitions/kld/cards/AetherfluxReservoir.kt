package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aetherflux Reservoir — Kaladesh #192 (canonical printing)
 * {4} · Artifact
 *
 * Whenever you cast a spell, you gain 1 life for each spell you've cast this turn.
 * Pay 50 life: This artifact deals 50 damage to any target.
 *
 * The life gain reads `DynamicAmounts.spellsCastThisTurn(You)` as the trigger resolves: the
 * triggering spell is already in the turn's cast history and counts, as do spells cast earlier in
 * the turn (even before the Reservoir was on the battlefield, even if countered) and spells cast in
 * response to the trigger — exactly the card's rulings ("cast a spell, then respond with a second
 * spell, you'll gain 4 life total").
 */
val AetherfluxReservoir = card("Aetherflux Reservoir") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever you cast a spell, you gain 1 life for each spell you've cast this turn.\n" +
        "Pay 50 life: This artifact deals 50 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.GainLife(DynamicAmounts.spellsCastThisTurn(Player.You))
    }

    activatedAbility {
        cost = Costs.PayLife(50)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(50, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Cliff Childs"
        flavorText = "While most power is used for ordinary, everyday tasks, one shouldn't underestimate its potential for the extraordinary."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96b6b2e1-c3e6-464c-8a13-b15deb34e862.jpg?1783937164"
        ruling("2016-09-20", "The first ability counts the spell that caused it to trigger plus any other spells you cast earlier in the turn.")
        ruling("2016-09-20", "The first ability counts spells you cast earlier in the turn even if you didn't control Aetherflux Reservoir as you cast them, and even if those spells were countered.")
        ruling("2016-09-20", "The number of spells you've cast is counted only as Aetherflux Reservoir's triggered ability resolves. For example, if you cast a spell, then respond to the triggered ability with a second spell, you'll gain 4 life total.")
        ruling("2016-09-20", "Aetherflux Reservoir's first ability doesn't trigger when you cast Aetherflux Reservoir itself.")
    }
}
