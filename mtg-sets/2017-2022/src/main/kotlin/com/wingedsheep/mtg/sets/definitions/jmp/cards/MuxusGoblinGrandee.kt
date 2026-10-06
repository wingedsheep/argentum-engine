package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Muxus, Goblin Grandee
 * {4}{R}{R}
 * Legendary Creature — Goblin Noble
 * 4/4
 * When Muxus enters, reveal the top six cards of your library. Put all Goblin creature cards with
 * mana value 5 or less from among them onto the battlefield and the rest on the bottom of your
 * library in a random order.
 * Whenever Muxus attacks, it gets +1/+1 until end of turn for each other Goblin you control.
 *
 * The ETB is a choice-free partition ("put all"), not a selection. The attack bonus is counted once
 * as the trigger resolves (per ruling) and locked in by `ModifyStats`. "Goblin you control" is a
 * bare tribal noun, so it counts Goblin permanents, not just creatures.
 */
val MuxusGoblinGrandee = card("Muxus, Goblin Grandee") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Goblin Noble"
    power = 4
    toughness = 4
    oracleText = "When Muxus enters, reveal the top six cards of your library. Put all Goblin creature cards " +
        "with mana value 5 or less from among them onto the battlefield and the rest on the bottom of your " +
        "library in a random order.\nWhenever Muxus attacks, it gets +1/+1 until end of turn for each other " +
        "Goblin you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(6), Player.You))
            reveal(revealed)
            val (goblins, rest) = filterSplit(
                revealed,
                GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN).manaValueAtMost(5)
            )
            move(goblins, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            toLibraryBottom(rest, order = CardOrder.Random)
        }
        description = "When Muxus enters, reveal the top six cards of your library. Put all Goblin creature " +
            "cards with mana value 5 or less from among them onto the battlefield and the rest on the bottom " +
            "of your library in a random order."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val otherGoblins = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN),
            excludeSelf = true
        ).count()
        effect = Effects.ModifyStats(
            power = otherGoblins,
            toughness = otherGoblins,
            target = EffectTarget.Self
        )
        description = "Whenever Muxus attacks, it gets +1/+1 until end of turn for each other Goblin you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c716d10-2130-43b7-a939-349d437e1091.jpg?1783930502"
        ruling("2020-06-23", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-06-23", "The bonus Muxus gets is determined only as its last ability resolves. Once that happens, the bonus won't change later in the turn even if the number of Goblins you control changes.")
    }
}
