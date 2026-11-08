package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rev, Tithe Extractor — Foundations Jumpstart #45
 * {3}{B} · Legendary Creature — Human Rogue · 3/3
 *
 * Whenever you attack, target creature gains deathtouch until end of turn.
 * Whenever one or more creatures you control deal combat damage to a player, create a Treasure
 * token, then look at the top card of that player's library and exile it face down. You may cast
 * that card for as long as it remains exiled.
 *
 * The attack trigger is "whenever you attack" (once per combat) over any creature. The damage
 * trigger is the per-damaged-player combat batch, binding the player hit as
 * `Player.TriggeringPlayer`; it then runs Gonti, Night Minister's steal — look at that player's
 * top card, exile it [FaceDownMode.HIDDEN], and grant its controller a permanent may-play — with
 * `nonLandOnly` for the "cast" wording and no mana-type rider (Rev has none). The grant goes to
 * the ability's controller at resolution, so it survives Rev leaving play or changing control.
 */
val RevTitheExtractor = card("Rev, Tithe Extractor") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Rogue"
    power = 3
    toughness = 3
    oracleText = "Whenever you attack, target creature gains deathtouch until end of turn.\n" +
        "Whenever one or more creatures you control deal combat damage to a player, create a " +
        "Treasure token, then look at the top card of that player's library and exile it face " +
        "down. You may cast that card for as long as it remains exiled. (A Treasure token is an " +
        "artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, creature)
        description = "Whenever you attack, target creature gains deathtouch until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.Pipeline {
            run(Effects.CreateTreasure(1))
            val topCard = gather(CardSource.TopOfLibrary(count = 1, player = Player.TriggeringPlayer))
            exile(topCard, Player.TriggeringPlayer, faceDown = FaceDownMode.HIDDEN)
            run(Effects.GrantMayPlayFromExile(
                from = topCard,
                expiry = MayPlayExpiry.Permanent,
                nonLandOnly = true,
            ))
        }
        description = "Whenever one or more creatures you control deal combat damage to a " +
            "player, create a Treasure token, then look at the top card of that player's " +
            "library and exile it face down. You may cast that card for as long as it remains " +
            "exiled."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "M.Matsumoto"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bf88010-799d-4217-8e79-308bec7ad2ff.jpg?1783908857"

        ruling("2024-11-08", "You may look at and cast cards exiled face down with Rev's last " +
            "ability even if Rev leaves the battlefield. If another player gains control of Rev, " +
            "that player can't look at or cast the cards you exiled with its last ability, and " +
            "you still can.")
        ruling("2024-11-08", "An effect that instructs you to \"cast\" a card doesn't allow you " +
            "to play lands.")
        ruling("2024-11-08", "You pay all costs and follow all timing rules for spells cast with " +
            "the permission granted by Rev's last ability. For example, if the exiled card is a " +
            "sorcery, you may cast it only during your main phase while the stack is empty.")
    }
}
