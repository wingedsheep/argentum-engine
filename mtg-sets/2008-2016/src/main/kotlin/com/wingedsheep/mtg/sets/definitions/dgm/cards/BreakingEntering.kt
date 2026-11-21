package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Breaking // Entering
 * {U}{B} Sorcery // {4}{B}{R} Sorcery
 * Breaking: Target player mills eight cards.
 * Entering: Put a creature card from a graveyard onto the battlefield under your control.
 * It gains haste until end of turn.
 * Fuse
 *
 * Entering doesn't target: the creature card is chosen from every graveyard (`Player.Each`) as it
 * resolves, so fused, Breaking's milled cards are already eligible.
 */
val BreakingEntering = card("Breaking // Entering") {
    layout = CardLayout.SPLIT
    colorIdentity = "UBR"
    keywords(Keyword.FUSE)

    face("Breaking") {
        manaCost = "{U}{B}"
        typeLine = "Sorcery"
        oracleText = "Target player mills eight cards.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            val player = target(Targets.Player)
            effect = Patterns.Library.mill(8, player)
        }
    }

    face("Entering") {
        manaCost = "{4}{B}{R}"
        typeLine = "Sorcery"
        oracleText = "Put a creature card from a graveyard onto the battlefield under your control. It gains haste until end of turn.\nFuse (You may cast one or both halves of this card from your hand.)"
        spell {
            effect = Effects.Pipeline {
                val candidates = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Creature)
                )
                val reanimated = chooseExactly(
                    1,
                    from = candidates,
                    showAllCards = true,
                    prompt = "Put a creature card from a graveyard onto the battlefield under your control"
                )
                move(reanimated, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                ifNotEmpty(reanimated) {
                    run(
                        Effects.GrantKeyword(
                            keyword = Keyword.HASTE,
                            target = reanimated.asTarget,
                            duration = Duration.EndOfTurn
                        )
                    )
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66724f4e-59dd-4c70-b09b-49947320e6d1.jpg?1783940017"
        ruling("2013-04-15", "If you're casting a split card with fuse from any zone other than your hand, you can't cast both halves. You'll only be able to cast one half or the other.")
        ruling("2013-04-15", "To cast a fused split spell, pay both of its mana costs. While the spell is on the stack, its mana value is the total amount of mana in both costs.")
        ruling("2013-04-15", "You can choose the same object as the target of each half of a fused split spell, if appropriate.")
        ruling("2013-04-15", "When resolving a fused split spell with multiple targets, treat it as you would any spell with multiple targets. If all targets are illegal when the spell tries to resolve, the spell doesn't resolve and none of its effects happen. If at least one target is still legal at that time, the spell resolves, but an illegal target can't perform any actions or have any actions performed on it.")
        ruling("2013-04-15", "When a fused split spell resolves, follow the instructions of the left half first, then the instructions on the right half.")
        ruling("2013-04-15", "On the stack, a split spell that hasn't been fused has only that half's characteristics and mana value. The other half is treated as though it didn't exist.")
        ruling("2013-04-15", "Some split cards with fuse have two monocolor halves of different colors. If such a card is cast as a fused split spell, the resulting spell is multicolored. If only one half is cast, the spell is the color of that half. While not on the stack, such a card is multicolored.")
        ruling("2013-04-15", "Some split cards with fuse have two halves that are both multicolored. That card is multicolored no matter which half is cast, or if both halves are cast. It's also multicolored while not on the stack.")
        ruling("2013-04-15", "If a player names a card, the player may name either half of a split card, but not both. A split card has the chosen name if one of its two names matches the chosen name.")
        ruling("2013-04-15", "If you cast a split card with fuse from your hand without paying its mana cost, you can choose to use its fuse ability and cast both halves without paying their mana costs.")
        ruling("2013-04-15", "Entering doesn't target any creature card. You choose which card you're putting onto the battlefield as Entering resolves. You can choose any creature card in a graveyard at time, including one put into a graveyard because of Breaking if you cast Breaking // Entering as a fused split spell.")
    }
}
