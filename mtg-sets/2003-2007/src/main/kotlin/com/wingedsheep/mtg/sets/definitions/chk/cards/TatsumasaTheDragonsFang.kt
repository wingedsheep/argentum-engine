package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tatsumasa, the Dragon's Fang
 * {6}
 * Legendary Artifact — Equipment
 * Equipped creature gets +5/+5.
 * {6}, Exile Tatsumasa: Create a 5/5 blue Dragon Spirit creature token with flying. Return
 * Tatsumasa to the battlefield under its owner's control when that token dies.
 * Equip {3}
 *
 * "When that token dies" is an event-based delayed trigger watching the token this resolution
 * just made ([CREATED_TOKENS]), with no turn-boundary expiry — the token may live for many turns.
 * The return moves `Self`: the Equipment exiled by its own activation cost is the object the
 * ability followed into exile, and the delayed trigger keeps that identity (CR 603.7c), so the
 * card comes back only if it is still the same exiled object when the token dies — one that left
 * exile in between is a new object (CR 400.7) and stays where it is.
 */
val TatsumasaTheDragonsFang = card("Tatsumasa, the Dragon's Fang") {
    manaCost = "{6}"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +5/+5.\n" +
        "{6}, Exile Tatsumasa: Create a 5/5 blue Dragon Spirit creature token with flying. " +
        "Return Tatsumasa to the battlefield under its owner's control when that token dies.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(5, 5)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.ExileSelf)
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Dragon", "Spirit"),
            keywords = setOf(Keyword.FLYING)
        ) then Effects.CreateDelayedTrigger(
            trigger = Triggers.self.dies(),
            watchedTarget = EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.Never,
            effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.EXILE)
        )
        description = "{6}, Exile Tatsumasa: Create a 5/5 blue Dragon Spirit creature token with " +
            "flying. Return Tatsumasa to the battlefield under its owner's control when that token dies."
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "270"
        artist = "Martina Pilcerova"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98d3bc63-8814-46e7-a6ee-dd5b94a8257e.jpg?1783944276"
    }
}
