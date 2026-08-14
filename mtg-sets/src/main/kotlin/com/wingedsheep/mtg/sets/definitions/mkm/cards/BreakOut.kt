package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Break Out {R}{G}
 * Sorcery
 *
 * Look at the top six cards of your library. You may reveal a creature card from among them. If
 * that card has mana value 2 or less, you may put it onto the battlefield and it gains haste until
 * end of turn. If you didn't put the revealed card onto the battlefield this way, put it into your
 * hand. Put the rest on the bottom of your library in a random order.
 *
 * Two nested "may"s over one revealed card, so the pipeline partitions rather than branches:
 *
 *  1. `chooseUpToSplit` of 1 creature card — the optional reveal. Declining leaves `revealed`
 *     empty and every later step is a no-op over an empty slot, so all six cards fall through to
 *     the bottom-of-library move.
 *  2. `filterSplit` on [CollectionFilter.ManaValueAtMost] splits the revealed card into the
 *     battlefield-eligible `cheap` slot and `tooExpensive`. The mana-value test is *not* a player
 *     choice, matching the card: only "you may put it onto the battlefield" is optional.
 *  3. A second `chooseUpToSplit` over `cheap` is that second "may". What the player declines lands
 *     in `declined` and joins `tooExpensive` on the way to hand — which is exactly the card's "if
 *     you didn't put the revealed card onto the battlefield this way, put it into your hand",
 *     covering both reasons it might not have been put there.
 *
 * Haste is granted per entered card via [ForEachInCollectionEffect] over the tracked move, so it
 * attaches to the permanent that actually entered rather than to the spell's controller, and
 * [Duration.EndOfTurn] expires it with the generic cleanup.
 *
 * The `reveal` step sits right after the selection rather than riding the later moves, so the card
 * becomes public when the player reveals it — before it's known whether it ends up on the
 * battlefield or in hand. `revealToSelf = false` skips the overlay for the controller, who just
 * looked at all six.
 */
val BreakOut = card("Break Out") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Look at the top six cards of your library. You may reveal a creature card from " +
        "among them. If that card has mana value 2 or less, you may put it onto the battlefield " +
        "and it gains haste until end of turn. If you didn't put the revealed card onto the " +
        "battlefield this way, put it into your hand. Put the rest on the bottom of your library " +
        "in a random order."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(6)))
            val (revealed, rest) = chooseUpToSplit(
                count = 1,
                from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may reveal a creature card",
                showAllCards = true
            )
            reveal(revealed, revealToSelf = false)
            val (cheap, tooExpensive) = filterSplit(
                from = revealed,
                filter = CollectionFilter.ManaValueAtMost(DynamicAmount.Fixed(2))
            )
            val (toBattlefield, declined) = chooseUpToSplit(
                count = 1,
                from = cheap,
                prompt = "You may put it onto the battlefield with haste"
            )
            val entered = moveTracked(
                from = toBattlefield,
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            )
            run(
                ForEachInCollectionEffect(
                    collection = entered.key,
                    effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self, Duration.EndOfTurn)
                )
            )
            toHand(tooExpensive)
            toHand(declined)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Daniel Correia"
        flavorText = "\"Hey, kiddo. Your ride's here.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c628476-0987-47d4-8d2a-cfc3977b2357.jpg?1783912863"
    }
}
