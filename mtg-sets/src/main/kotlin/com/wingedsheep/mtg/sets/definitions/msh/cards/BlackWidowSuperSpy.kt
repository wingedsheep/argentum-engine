package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Black Widow, Super Spy — Marvel Super Heroes #89
 * {1}{B} · Legendary Creature — Human Spy Hero · 2/1
 *
 * Menace
 * Whenever Black Widow deals combat damage to a player, that player exiles cards from the top of
 * their library until they exile a nonland card. You may put a +1/+1 counter on Black Widow. If
 * you don't, you may cast the exiled nonland card until end of turn and mana of any type can be
 * spent to cast that spell.
 *
 * Implementation notes:
 * - The exile is the Gather → Move pipeline over the *damaged* player's library
 *   ([Player.TriggeringPlayer]): [GatherUntilMatchEffect] walks the library top-down until it hits
 *   a nonland card, storing the stopping card (`widowNonland`) and every card it walked past
 *   (`widowExiled`, which includes the nonland). The whole walked run is exiled — the lands seen
 *   on the way are exiled too, and a library with no nonland card at all is exiled entirely,
 *   leaving `widowNonland` empty so the may-cast grant below is a no-op.
 * - "You may … If you don't, …" is a [MayEffect] with an `otherwise` branch: taking the counter
 *   *is* the decision, so declining (not failing) is what hands over the cast permission. The
 *   grant is Ragavan's [Effects.GrantMayPlayFromExile] — a normal-cost "may cast", so all timing
 *   restrictions and costs still apply — plus `withAnyManaType` for the "mana of any type" rider
 *   and `nonLandOnly` to keep the permission from ever covering a land. The gate is on the
 *   *decision*, so answering "yes" while Black Widow has already left the battlefield puts no
 *   counter anywhere and grants no cast — the player chose the counter.
 * - Collections track entity ids, which survive the zone change, so the grant can name the
 *   `widowNonland` collection gathered before the move.
 */
val BlackWidowSuperSpy = card("Black Widow, Super Spy") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Spy Hero"
    power = 2
    toughness = 1
    oracleText = "Menace\n" +
        "Whenever Black Widow deals combat damage to a player, that player exiles cards from " +
        "the top of their library until they exile a nonland card. You may put a +1/+1 counter " +
        "on Black Widow. If you don't, you may cast the exiled nonland card until end of turn " +
        "and mana of any type can be spent to cast that spell."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Composite(
            GatherUntilMatchEffect(
                player = Player.TriggeringPlayer,
                filter = GameObjectFilter.Nonland,
                storeMatch = "widowNonland",
                storeRevealed = "widowExiled",
            ),
            MoveCollectionEffect(
                from = "widowExiled",
                destination = CardDestination.ToZone(Zone.EXILE, Player.TriggeringPlayer),
            ),
            MayEffect(
                effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                descriptionOverride = "Put a +1/+1 counter on Black Widow",
                otherwise = Effects.GrantMayPlayFromExile(
                    from = "widowNonland",
                    expiry = MayPlayExpiry.EndOfTurn,
                    withAnyManaType = true,
                    nonLandOnly = true,
                ),
            ),
        )
        description = "Whenever Black Widow deals combat damage to a player, that player exiles " +
            "cards from the top of their library until they exile a nonland card. You may put a " +
            "+1/+1 counter on Black Widow. If you don't, you may cast the exiled nonland card " +
            "until end of turn and mana of any type can be spent to cast that spell."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "89"
        artist = "Dan Brereton"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63ce0909-7d7d-410d-ab6c-c87fa3e23877.jpg?1783902946"
    }
}
