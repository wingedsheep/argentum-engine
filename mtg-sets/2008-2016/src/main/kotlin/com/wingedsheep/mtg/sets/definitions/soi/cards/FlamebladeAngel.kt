package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flameblade Angel — Shadows over Innistrad #157
 * {4}{R}{R} · Creature — Angel 4/4
 *
 * Flying
 * Whenever a source an opponent controls deals damage to you or a permanent you control, you may
 * have this creature deal 1 damage to that source's controller.
 *
 * Elesh Norn's join: the oracle text spans two recipient kinds, so the punisher is two triggers —
 * `Recipient.You` and `Recipient.PermanentYouControl` — each firing once per damaged recipient per
 * source, exactly as the rulings below describe (trample over a blocker and onto you triggers twice).
 * "That source's controller" is `Player.ControllerOfTriggeringEntity` (the damage source), read as
 * last-known control when a burn spell has already left the stack. The damage is untargeted and dealt
 * by the Angel itself (the ability's source). "You may" is a resolution-time yes/no (`optional`), which
 * is what ends the two-Angel ping-pong loop from the second ruling.
 */
val FlamebladeAngel = card("Flameblade Angel") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever a source an opponent controls deals damage to you or a permanent you control, you may " +
        "have this creature deal 1 damage to that source's controller."

    keywords(Keyword.FLYING)

    val ping = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.ControllerOfTriggeringEntity))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.opponentControls()).dealsDamage(Recipient.You)
        optional = true
        effect = ping
        description = "Whenever a source an opponent controls deals damage to you, you may have " +
            "this creature deal 1 damage to that source's controller."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.opponentControls()).dealsDamage(Recipient.PermanentYouControl)
        optional = true
        effect = ping
        description = "Whenever a source an opponent controls deals damage to a permanent you control, " +
            "you may have this creature deal 1 damage to that source's controller."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Cynthia Sheppard"
        flavorText = "\"You must pay for your sin in blood.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1031ec22-c99f-4843-a7cf-c8332234eca8.jpg?1783937753"
        ruling(
            "2016-04-08",
            "If a source deals damage to you and/or one or more permanents you control at the same time, " +
                "Flameblade Angel's last ability will trigger that many times. For example, if a creature " +
                "with trample deals damage to a blocking creature you control and you, Flameblade Angel's " +
                "ability will trigger two times."
        )
        ruling(
            "2016-04-08",
            "Note that the last ability is optional. Say two players each control a Flameblade Angel, and a " +
                "source controlled by the first player deals damage to the second player or a permanent they " +
                "control. The resulting triggered ability may deal damage to the first player, causing the " +
                "ability of that player's Flameblade Angel to trigger. That ability may deal damage to the " +
                "second player, and so on. This cycle will repeat until the game ends or one player declines " +
                "to use the ability, putting an end to the carnage."
        )
    }
}
