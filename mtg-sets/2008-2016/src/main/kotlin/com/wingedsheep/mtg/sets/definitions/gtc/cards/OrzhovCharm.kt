package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Orzhov Charm
 * {W}{B}
 * Instant
 * Choose one —
 * • Return target creature you control and all Auras you control attached to it to their owner's hand.
 * • Destroy target creature and you lose life equal to its toughness.
 * • Return target creature card with mana value 1 or less from your graveyard to the battlefield.
 */
val OrzhovCharm = card("Orzhov Charm") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Return target creature you control and all Auras you control attached to it to their owner's hand.\n" +
        "• Destroy target creature and you lose life equal to its toughness.\n" +
        "• Return target creature card with mana value 1 or less from your graveyard to the battlefield."

    spell {
        modal(chooseCount = 1) {
            mode("Return target creature you control and all Auras you control attached to it to their owner's hand") {
                val creature = target(TargetFilter.CreatureYouControl)
                effect = Effects.Pipeline {
                    // The Auras aren't targeted; gather them before the host leaves.
                    val auras = gather(
                        CardSource.AttachedTo(
                            host = creature,
                            filter = GameObjectFilter.Permanent.withSubtype(Subtype.AURA).youControl(),
                        )
                    )
                    run(Effects.ReturnToHand(creature))
                    toHand(auras)
                }
            }
            mode("Destroy target creature and you lose life equal to its toughness") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Pipeline {
                    // Toughness as last on the battlefield; life is lost even if it isn't destroyed.
                    val toughness = storeNumber(DynamicAmounts.toughnessOf(creature))
                    run(Effects.Destroy(creature))
                    run(Effects.LoseLife(toughness.amount, EffectTarget.Controller))
                }
            }
            mode("Return target creature card with mana value 1 or less from your graveyard to the battlefield") {
                val creatureCard = target(
                    TargetFilter.CreatureInYourGraveyard.manaValueAtMost(1)
                )
                effect = Effects.PutOntoBattlefield(creatureCard)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8ca44265-5e1b-4fbf-9002-52b2ce9b7448.jpg?1783940104"
        ruling("2013-07-01", "If you choose the second mode, and Orzhov Charm resolves but the creature isn't destroyed (perhaps because it has indestructible or it regenerates), you'll still lose life equal to the creature's toughness.")
        ruling("2013-01-24", "Orzhov Charm's first mode doesn't target any of the Auras attached to the target creature.")
        ruling("2013-01-24", "If you choose the first mode, any Aura controlled by another player attached to the creature will be put into its owner's graveyard as a state-based action after the creature leaves the battlefield.")
        ruling("2013-01-24", "If you choose the second mode, you'll lose life equal to the creature's toughness when it was last on the battlefield.")
        ruling("2013-01-24", "For each of the modes, if the target creature or creature card is an illegal target when Orzhov Charm tries to resolve, it won't resolve and none of its effects will happen.")
    }
}
