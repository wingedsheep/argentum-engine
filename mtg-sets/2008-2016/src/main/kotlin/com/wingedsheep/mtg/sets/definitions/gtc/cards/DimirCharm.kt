package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dimir Charm
 * {U}{B}
 * Instant
 * Choose one —
 * • Counter target sorcery spell.
 * • Destroy target creature with power 2 or less.
 * • Look at the top three cards of target player's library. Put one back and the rest into that
 *   player's graveyard.
 *
 * The third mode is the caster's choice: exactly one of the looked-at cards goes back on top of the
 * targeted player's library, the others into that player's graveyard.
 */
val DimirCharm = card("Dimir Charm") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Counter target sorcery spell.\n" +
        "• Destroy target creature with power 2 or less.\n" +
        "• Look at the top three cards of target player's library. Put one back and the rest into that player's graveyard."

    spell {
        modal(chooseCount = 1) {
            mode("Counter target sorcery spell") {
                target(TargetFilter.SorcerySpellOnStack)
                effect = Effects.CounterSpell()
            }
            mode("Destroy target creature with power 2 or less") {
                val t = target(TargetFilter.Creature.powerAtMost(2))
                effect = Effects.Destroy(t)
            }
            mode("Look at the top three cards of target player's library. Put one back and the rest into that player's graveyard") {
                val player = target(Targets.Player)
                effect = Effects.Pipeline {
                    val looked = gather(CardSource.TopOfLibrary(3, player.asPlayer))
                    val (toTop, toGraveyardCards) = chooseExactlySplit(
                        1,
                        from = looked,
                        selectedLabel = "Put back on top",
                        remainderLabel = "Put into graveyard"
                    )
                    toLibraryTop(toTop, player.asPlayer, order = CardOrder.Preserve)
                    toGraveyard(toGraveyardCards, player.asPlayer)
                }
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Zoltan Boros"
        flavorText = "\"Dangerous to recognize. Deadly not to.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3f4cfa7-8ee4-4a85-9e6a-65a7541f62c1.jpg?1783940109"
    }
}
