package com.wingedsheep.mtg.sets.definitions.sok.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Michiko Konda, Truth Seeker — Saviors of Kamigawa #19
 * {3}{W} · Legendary Creature — Human Advisor 2/2
 *
 * Whenever a source an opponent controls deals damage to you, that player sacrifices a permanent
 * of their choice.
 *
 * Elesh Norn's join: `Triggers.a(Any.opponentControls()).dealsDamage(Recipient.You)` binds the
 * damage *source* as the triggering entity, so "that player" is `Player.ControllerOfTriggeringEntity`
 * — last-known control when a burn spell has already left the stack by resolution. The trigger fires
 * once per damage event (rulings below), and one permanent is sacrificed regardless of the amount.
 *
 * Reprinted in Jumpstart 2022 (Printing row there).
 */
val MichikoKondaTruthSeeker = card("Michiko Konda, Truth Seeker") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Advisor"
    power = 2
    toughness = 2
    oracleText = "Whenever a source an opponent controls deals damage to you, that player sacrifices " +
        "a permanent of their choice."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.opponentControls()).dealsDamage(Recipient.You)
        effect = Effects.Sacrifice(
            GameObjectFilter.Any,
            1,
            EffectTarget.PlayerRef(Player.ControllerOfTriggeringEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Christopher Moeller"
        flavorText = "\"Watch over my father. Tell him I'm safe, but I won't come home until I find out " +
            "how to bring him back to his senses, and Kamigawa is again at peace.\"\n" +
            "—Michiko Konda, last letter to General Takeno"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99c56bf0-ad9f-4419-902c-f3a3880c716c.jpg?1783944168"
        ruling(
            "2005-06-01",
            "One permanent is sacrificed each time an opponent's source deals damage to Michiko Konda's " +
                "controller. The amount of damage doesn't matter."
        )
        ruling(
            "2005-06-01",
            "Sources that deal damage multiple times (such as creatures with double strike) will trigger " +
                "Michiko Konda multiple times."
        )
    }
}
