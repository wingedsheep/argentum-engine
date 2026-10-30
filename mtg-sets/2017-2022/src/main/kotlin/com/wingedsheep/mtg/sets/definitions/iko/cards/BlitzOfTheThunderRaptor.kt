package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Blitz of the Thunder-Raptor
 * {1}{R}
 * Instant
 * Blitz of the Thunder-Raptor deals damage to target creature or planeswalker equal to the number
 * of instant and sorcery cards in your graveyard. If that creature or planeswalker would die this
 * turn, exile it instead.
 *
 * The exile mark is applied before the damage so state-based actions see it (Carbonize's shape),
 * and it applies even when the damage is zero (2020-04-17 ruling). The spell itself is on the stack
 * while the count is taken, so it never counts itself.
 */
val BlitzOfTheThunderRaptor = card("Blitz of the Thunder-Raptor") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Blitz of the Thunder-Raptor deals damage to target creature or planeswalker equal to the " +
        "number of instant and sorcery cards in your graveyard. If that creature or planeswalker would die " +
        "this turn, exile it instead."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.MarkExileOnDeath(t) then
            Effects.DealDamage(DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Zack Stella"
        flavorText = "The sound of Vadrok's gigantic wings is ominous. But the true terror is when they fall silent."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0174556-9f9b-4756-84ca-d14a5a60720c.jpg?1783931053"
        ruling("2020-04-17", "Blitz of the Thunder-Raptor is still on the stack while you determine how much damage is dealt. It isn't in your graveyard to contribute to that amount of damage.")
        ruling("2020-04-17", "Blitz of the Thunder-Raptor's replacement effect will exile the target creature or planeswalker if it would die this turn for any reason, not just due to lethal damage. It applies to the target creature or planeswalker even if Blitz of the Thunder-Raptor deals no damage to it (most likely because you have no instant or sorcery cards in your graveyard).")
    }
}
