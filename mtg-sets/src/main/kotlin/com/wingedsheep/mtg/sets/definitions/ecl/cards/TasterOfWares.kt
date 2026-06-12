package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Taster of Wares
 * {2}{B}
 * Creature — Goblin Warlock
 * 3/2
 *
 * When this creature enters, target opponent reveals X cards from their hand,
 * where X is the number of Goblins you control. You choose one of those cards.
 * That player exiles it. If an instant or sorcery card is exiled this way, you
 * may cast it for as long as you control this creature, and mana of any type
 * can be spent to cast that spell.
 *
 * Implementation notes:
 * - The cast-from-exile permission is modeled with `permanent = true`, which
 *   keeps it active as long as the card stays in exile. The "as long as you
 *   control this creature" clause is a small simplification (matches the typical
 *   case where the player casts the exiled spell while Taster is still alive).
 * - "Mana of any type can be spent" is plumbed via `withAnyManaType = true` on
 *   the granted permission, which relaxes colored cost requirements at cast time.
 */
val TasterOfWares = card("Taster of Wares") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Goblin Warlock"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, target opponent reveals X cards from their hand, where X is the number of Goblins you control. You choose one of those cards. That player exiles it. If an instant or sorcery card is exiled this way, you may cast it for as long as you control this creature, and mana of any type can be spent to cast that spell."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("opponent", TargetOpponent())
        description = "When this creature enters, target opponent reveals X cards from their hand, " +
            "where X is the number of Goblins you control. You choose one of those cards. " +
            "That player exiles it. If an instant or sorcery card is exiled this way, you may cast it " +
            "for as long as you control this creature, and mana of any type can be spent to cast that spell."
        effect = Effects.Pipeline {
            // Snapshot the number of Goblins you control as X
            val goblinCount = storeNumber(
                DynamicAmounts.battlefield(
                    Player.You,
                    GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)
                ).count(),
                name = "goblinCount"
            )
            // Gather opponent's hand
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), name = "hand")
            // Opponent chooses X cards to reveal
            val revealed = chooseExactly(
                DynamicAmount.VariableReference("goblinCount"),
                from = hand,
                chooser = Chooser.TargetPlayer,
                prompt = "Reveal X cards from your hand (X = number of Goblins your opponent controls)",
                name = "revealed"
            )
            // You choose one of the revealed cards (use ChooseUpTo so X=0
            // and empty-hand cases resolve cleanly)
            val chosen = chooseUpTo(
                1, from = revealed,
                chooser = Chooser.Controller,
                prompt = "Choose one of the revealed cards to exile",
                name = "chosen"
            )
            // That player exiles it
            move(chosen, CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0)))
            // If the exiled card is an instant or sorcery, grant cast-from-exile
            val instantOrSorcery = filter(
                chosen,
                CollectionFilter.MatchesFilter(GameObjectFilter.InstantOrSorcery),
                name = "instantOrSorcery"
            )
            run(
                GrantMayPlayFromExileEffect(
                    from = "instantOrSorcery",
                    expiry = MayPlayExpiry.Permanent,
                    withAnyManaType = true
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc0b64b6-8984-431c-8a2f-84402b429e2b.jpg?1767952030"
    }
}
