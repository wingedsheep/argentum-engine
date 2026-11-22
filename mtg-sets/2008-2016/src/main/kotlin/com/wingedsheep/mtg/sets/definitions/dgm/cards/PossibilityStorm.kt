package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Possibility Storm — Dragon's Maze #34
 * {3}{R}{R} · Enchantment · Rare
 *
 * Whenever a player casts a spell from their hand, that player exiles it, then exiles cards from
 * the top of their library until they exile a card that shares a card type with it. That player
 * may cast that card without paying its mana cost. Then they put all cards exiled with this
 * enchantment on the bottom of their library in a random order.
 *
 * Everything is done by the caster, not by Possibility Storm's controller, so the whole body runs
 * under `ForEachPlayer(Player.TriggeringPlayer)` (the Eye of the Storm shape): inside it `Player.You`,
 * the `May` decision and the free cast's caster all rebind to the player who cast the spell, while
 * `EffectTarget.TriggeringEntity` still names that spell.
 *   1. [Effects.ExileTriggeringSpell] exiles the spell from the stack (not a counter) and links it
 *      to this enchantment — a no-op if it already left the stack (another Possibility Storm).
 *   2. `gatherUntilMatch` walks that player's library to the first card sharing a card type with
 *      the spell (`sharingCardTypeWith(TriggeringEntity)`, which still reads the card in exile, or
 *      its last-known types), and every card seen is exiled, linked to this enchantment.
 *   3. That player may cast the hit for free.
 *   4. Everything still exiled with this enchantment — the original spell, the cards skipped, and
 *      the hit if it wasn't cast (the cast one has moved to the stack) — goes to the bottom of
 *      that player's library in a random order.
 */
val PossibilityStorm = card("Possibility Storm") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a player casts a spell from their hand, that player exiles it, then " +
        "exiles cards from the top of their library until they exile a card that shares a card " +
        "type with it. That player may cast that card without paying its mana cost. Then they put " +
        "all cards exiled with this enchantment on the bottom of their library in a random order."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)))
        effect = Effects.ForEachPlayer(
            Player.TriggeringPlayer,
            Effects.Pipeline {
                run(Effects.ExileTriggeringSpell(linkToSource = true))
                val (hit, revealed) = gatherUntilMatch(
                    GameObjectFilter.Any.sharingCardTypeWith(EffectTarget.TriggeringEntity)
                )
                exile(revealed, linkToSource = true)
                ifNotEmpty(hit) {
                    run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(hit)))
                }
                val exiledWithStorm = gather(CardSource.FromLinkedExile())
                toLibraryBottom(exiledWithStorm, order = CardOrder.Random)
            }
        )
        description = "Whenever a player casts a spell from their hand, that player exiles it, then " +
            "exiles cards from the top of their library until they exile a card that shares a card " +
            "type with it. That player may cast that card without paying its mana cost. Then they " +
            "put all cards exiled with this enchantment on the bottom of their library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/858aa831-b491-4f1e-bb56-33eeca14771d.jpg?1783940037"
        ruling("2013-04-15", "The original spell is part of the group of exiled cards put on the bottom of the library in a random order. If the exiled card that shared a card type with that card wasn't cast, it's also part of this group.")
        ruling("2013-04-15", "If Possibility Storm's ability doesn't exile the original spell (perhaps because another Possibility Storm already exiled it), you'll still exile cards from the top of your library until you exile a card that shares a card type with it and have the opportunity to cast that spell.")
        ruling("2013-04-15", "If the spell you cast from exile has X in its mana cost, you must choose 0 as its value.")
    }
}
