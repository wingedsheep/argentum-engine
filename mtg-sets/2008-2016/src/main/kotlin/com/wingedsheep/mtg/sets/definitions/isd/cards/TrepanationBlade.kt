package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trepanation Blade
 * {3}
 * Artifact — Equipment
 * Whenever equipped creature attacks, defending player reveals cards from the top of their library
 * until they reveal a land card. The creature gets +1/+0 until end of turn for each card revealed
 * this way. That player puts the revealed cards into their graveyard.
 * Equip {2}
 *
 * The equipped creature's attack trigger (`Triggers.attached.attacks`) binds the attacker as
 * [EffectTarget.TriggeringEntity], so the bonus lands on that creature even if the Blade moves in
 * response. `gatherUntilMatch` walks the defending player's library ([Player.DefendingPlayer],
 * read off the equipped attacker — the planeswalker's controller when it attacks a planeswalker)
 * down to and including the first land; with no land, every card is revealed. The bonus counts the
 * whole revealed collection — land included (2017-11-17 ruling) — via the revealed slot's
 * `count`, and every revealed card goes to its owner's graveyard.
 */
val TrepanationBlade = card("Trepanation Blade") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature attacks, defending player reveals cards from the top of their " +
        "library until they reveal a land card. The creature gets +1/+0 until end of turn for each card " +
        "revealed this way. That player puts the revealed cards into their graveyard.\n" +
        "Equip {2}"

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.Pipeline {
            val (_, revealed) = gatherUntilMatch(GameObjectFilter.Land, player = Player.DefendingPlayer)
            reveal(revealed)
            run(Effects.ModifyStats(revealed.count, DynamicAmounts.fixed(0), EffectTarget.TriggeringEntity))
            move(revealed, CardDestination.ToZone(Zone.GRAVEYARD))
        }
        description = "Whenever equipped creature attacks, defending player reveals cards from the top of " +
            "their library until they reveal a land card. The creature gets +1/+0 until end of turn for " +
            "each card revealed this way. That player puts the revealed cards into their graveyard."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/2182be77-9186-4d16-a070-9577d4392999.jpg?1783940897"
        ruling(
            "2017-11-17",
            "The land card is counted when calculating the bonus, and it will be put into the graveyard " +
                "with the other revealed cards."
        )
        ruling(
            "2017-11-17",
            "If the equipped creature is attacking a planeswalker, the controller of the planeswalker is " +
                "the defending player."
        )
    }
}
