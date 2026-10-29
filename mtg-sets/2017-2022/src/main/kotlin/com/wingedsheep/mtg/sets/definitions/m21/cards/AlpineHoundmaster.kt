package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Alpine Houndmaster
 * {R}{W}
 * Creature — Human Warrior
 * 2/2
 * When this creature enters, you may search your library for a card named Alpine Watchdog and/or
 * a card named Igneous Cur, reveal them, put them into your hand, then shuffle.
 * Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of other
 * attacking creatures.
 *
 * The search is one search with one shuffle (the Tiamat pipeline): gather library cards named
 * either card → choose up to two under [SelectionRestriction.OnePerCardName], so at most one of
 * each name → reveal into hand → shuffle. The "you may" is the optional trigger — declining
 * skips the search and its shuffle entirely.
 *
 * X counts every other attacking creature, whoever controls it, and is locked in on resolution
 * (2020-06-23 ruling).
 */
val AlpineHoundmaster = card("Alpine Houndmaster") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may search your library for a card named Alpine " +
        "Watchdog and/or a card named Igneous Cur, reveal them, put them into your hand, then shuffle.\n" +
        "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of " +
        "other attacking creatures."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    GameObjectFilter.Any.named("Alpine Watchdog") or GameObjectFilter.Any.named("Igneous Cur"),
                ),
                search = true,
            )
            val found = chooseUpTo(
                2,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for a card named Alpine Watchdog and/or a card named Igneous Cur",
            )
            toHand(found, revealed = true)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
        description = "When this creature enters, you may search your library for a card named Alpine " +
            "Watchdog and/or a card named Igneous Cur, reveal them, put them into your hand, then shuffle."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(
            DynamicAmounts.battlefield(
                Player.Each,
                GameObjectFilter.Creature.attacking(),
                excludeSelf = true,
            ).count(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self,
        )
        description = "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the " +
            "number of other attacking creatures."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43224e74-2c51-40bd-bc34-f66e990a3e33.jpg?1783930663"
        ruling(
            "2020-06-23",
            "The value of X is determined only as Alpine Houndmaster's last ability resolves. Once " +
                "that happens, the value of X won't change later in the turn even if the number of " +
                "attacking creatures changes."
        )
    }
}
