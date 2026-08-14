package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Basilica Stalker — Murders at Karlov Manor #78
 * {5}{B} · Creature — Vampire Detective · 3/4
 *
 * Flying
 * Whenever this creature deals combat damage to a player, you gain 1 life and surveil 1.
 * Disguise {4}{B}
 *
 * Disguise here is a *rate* fix, not an evasion trick: {3} face down beats {5}{B} on curve, and the
 * {4}{B} flip is paid later, at instant speed, out of the same mana you'd otherwise hold up. While
 * face down it's a vanilla 2/2 with ward {2} (CR 702.168a) — the flying and the damage trigger are
 * both suppressed (CR 708.2) until it's turned face up.
 *
 * The trigger is SELF-bound and combat-only, so pinging a player with a non-combat damage effect
 * doesn't fire it. Its two clauses resolve in the printed order under one composite: the life comes
 * in before the surveil, which matters only for anything watching lifegain mid-resolution.
 */
val BasilicaStalker = card("Basilica Stalker") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Detective"
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, you gain 1 life and surveil 1. " +
        "(Look at the top card of your library. You may put it into your graveyard.)\n" +
        "Disguise {4}{B} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)"
    power = 3
    toughness = 4
    keywords(Keyword.FLYING)
    disguise = "{4}{B}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Composite(
            Effects.GainLife(1),
            Effects.Surveil(1)
        )
        description = "Whenever this creature deals combat damage to a player, you gain 1 life and surveil 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdafea8f-283d-4f19-a74a-669bfbdfed98.jpg?1783912901"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
    }
}
