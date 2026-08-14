package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bloodvial Purveyor
 * {2}{B}{B}
 * Creature — Vampire Noble
 * 5/4
 *
 * Flying, trample
 * Whenever an opponent casts a spell, that player creates a Blood token.
 * Whenever this creature attacks, it gets +1/+0 until end of turn for each Blood token defending
 * player controls.
 *
 * Implementation:
 *  - The opponent-cast trigger creates the Blood token *for the casting player*, so
 *    [Effects.CreateBlood] is controlled by [Player.TriggeringPlayer] (the opponent who cast the
 *    spell), mirroring "that player creates a Blood token."
 *  - The attack pump reads [DynamicAmounts.battlefield] scoped to [Player.DefendingPlayer] over the
 *    Blood-artifact filter (`GameObjectFilter.Artifact.withSubtype("Blood")`), applied as a
 *    +N/+0 self buff until end of turn.
 */
val BloodvialPurveyor = card("Bloodvial Purveyor") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Noble"
    power = 5
    toughness = 4
    oracleText = "Flying, trample\n" +
        "Whenever an opponent casts a spell, that player creates a Blood token. (It's an artifact " +
        "with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")\n" +
        "Whenever this creature attacks, it gets +1/+0 until end of turn for each Blood token " +
        "defending player controls."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    // Whenever an opponent casts a spell, that player creates a Blood token.
    triggeredAbility {
        trigger = Triggers.OpponentCastsSpell
        effect = Effects.CreateBlood(controller = EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent casts a spell, that player creates a Blood token."
    }

    // Whenever this creature attacks, it gets +1/+0 until end of turn for each Blood token
    // defending player controls.
    triggeredAbility {
        trigger = Triggers.Attacks
        val bloodDefender = DynamicAmounts.battlefield(
            Player.DefendingPlayer,
            GameObjectFilter.Artifact.withSubtype("Blood")
        ).count()
        effect = Effects.ModifyStats(
            power = bloodDefender,
            toughness = DynamicAmount.Fixed(0),
            target = EffectTarget.Self
        )
        description = "Whenever this creature attacks, it gets +1/+0 until end of turn for each " +
            "Blood token defending player controls."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "98"
        artist = "Fesbra"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4889c58b-8b84-42af-a56c-e886655aa997.jpg"
    }
}
