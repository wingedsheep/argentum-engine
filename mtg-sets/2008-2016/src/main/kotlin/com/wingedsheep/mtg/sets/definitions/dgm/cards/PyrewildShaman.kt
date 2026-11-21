package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pyrewild Shaman — Dragon's Maze #36 (canonical printing)
 * {2}{R} · Creature — Goblin Shaman · 3/1
 *
 * Bloodrush — {1}{R}, Discard this card: Target attacking creature gets +3/+1 until end of turn.
 * Whenever one or more creatures you control deal combat damage to a player, if this card is in
 * your graveyard, you may pay {3}. If you do, return this card to your hand.
 *
 * The recursion trigger works only from the graveyard ([Zone.GRAVEYARD] trigger zone) and carries an
 * intervening "if" re-checked on resolution, so it does nothing if the card has left the graveyard by
 * then. Same shape as Killian's Confidence.
 */
val PyrewildShaman = card("Pyrewild Shaman") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Shaman"
    power = 3
    toughness = 1
    oracleText = "Bloodrush — {1}{R}, Discard this card: Target attacking creature gets +3/+1 until end of turn.\n" +
        "Whenever one or more creatures you control deal combat damage to a player, if this card is in " +
        "your graveyard, you may pay {3}. If you do, return this card to your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(3, 1, creature)
        description = "Bloodrush — {1}{R}, Discard this card: Target attacking creature gets +3/+1 until end of turn."
    }

    triggeredAbility {
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.SourceInZone(Zone.GRAVEYARD)
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{3}"),
            then = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        )
        description = "Whenever one or more creatures you control deal combat damage to a player, if this " +
            "card is in your graveyard, you may pay {3}. If you do, return this card to your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c6f6e45-f613-420d-83d2-d93c643265ee.jpg?1783940037"
    }
}
