package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mishra's Command
 * {X}{R}
 * Sorcery
 * Choose two —
 * • Choose target player. They may discard up to X cards. Then they draw a card for each card
 *   discarded this way.
 * • This spell deals X damage to target creature.
 * • This spell deals X damage to target planeswalker.
 * • Target creature gets +X/+0 and gains haste until end of turn.
 *
 * Every mode reads the one X paid at cast time ([DynamicAmounts.xValue]), Profane Command's shape.
 * The first mode's "may discard up to X" is a resolution-time `chooseUpTo(X)` over the targeted
 * player's hand with that player as the chooser — choosing nothing is the decline, and the draw
 * counts exactly the cards that were discarded (so zero discards draws zero). Each mode resolves
 * against its own target list, so `Chooser.TargetPlayer` names the mode's player.
 */
val MishrasCommand = card("Mishra's Command") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose two —\n" +
        "• Choose target player. They may discard up to X cards. Then they draw a card for each card discarded this way.\n" +
        "• This spell deals X damage to target creature.\n" +
        "• This spell deals X damage to target planeswalker.\n" +
        "• Target creature gets +X/+0 and gains haste until end of turn."

    spell {
        modal(chooseCount = 2) {
            mode("Target player may discard up to X cards, then draws a card for each card discarded this way") {
                val player = target(Targets.Player)
                effect = Effects.Pipeline {
                    val hand = gather(CardSource.FromZone(Zone.HAND, player.asPlayer))
                    val discarded = chooseUpTo(
                        DynamicAmounts.xValue(),
                        from = hand,
                        chooser = Chooser.TargetPlayer,
                        prompt = "Discard up to X cards, then draw that many"
                    )
                    discard(discarded, player.asPlayer)
                    run(Effects.DrawCards(DynamicAmounts.distinctEntitiesIn(discarded), player))
                }
            }
            mode("This spell deals X damage to target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(DynamicAmounts.xValue(), creature)
            }
            mode("This spell deals X damage to target planeswalker") {
                val planeswalker = target(TargetFilter.Planeswalker)
                effect = Effects.DealDamage(DynamicAmounts.xValue(), planeswalker)
            }
            mode("Target creature gets +X/+0 and gains haste until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(DynamicAmounts.xValue(), DynamicAmounts.fixed(0), creature) then
                    Effects.GrantKeyword(Keyword.HASTE, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4d2e518-30c6-480c-aaf4-b7596c9479e4.jpg?1783920068"
    }
}
