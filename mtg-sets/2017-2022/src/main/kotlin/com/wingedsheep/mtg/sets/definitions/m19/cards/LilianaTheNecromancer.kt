package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana, the Necromancer
 * {3}{B}{B}
 * Legendary Planeswalker — Liliana
 * Loyalty: 4
 *
 * +1: Target player loses 2 life.
 * −1: Return target creature card from your graveyard to your hand.
 * −7: Destroy up to two target creatures. Put up to two creature cards from graveyards onto the
 * battlefield under your control.
 *
 * The −7 targets only the creatures it destroys (zero to two of them — Force of Vigor's optional
 * `count = 2` shape). The reanimation half is untargeted: it gathers creature cards from every
 * graveyard *after* the destruction, so the creatures just destroyed are among the cards you may
 * choose (2018-07-13 ruling), and the choice is made as the ability resolves with no window to
 * respond between choosing and returning.
 */
val LilianaTheNecromancer = card("Liliana, the Necromancer") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 4
    oracleText = "+1: Target player loses 2 life.\n" +
        "−1: Return target creature card from your graveyard to your hand.\n" +
        "−7: Destroy up to two target creatures. Put up to two creature cards from graveyards onto " +
        "the battlefield under your control."

    loyaltyAbility(+1) {
        val player = target(Targets.Player)
        effect = Effects.LoseLife(2, player)
    }

    loyaltyAbility(-1) {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(creatureCard)
    }

    loyaltyAbility(-7) {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0))) then
            Effects.Pipeline {
                val creatureCards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Creature))
                val chosen = chooseUpTo(
                    2,
                    from = creatureCards,
                    prompt = "Choose up to two creature cards to put onto the battlefield under your control"
                )
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "291"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d2cbc2c-dbe3-4ecb-8fbe-785cb80b9e76.jpg?1783934485"
        ruling(
            "2018-07-13",
            "For Liliana's last ability, the creatures you'll return to the battlefield are chosen after " +
                "the target creatures are destroyed. No player may take actions between the time you choose " +
                "two creature cards to return and the time they're returned. The destroyed cards can be among " +
                "those returned to the battlefield."
        )
        ruling(
            "2018-07-13",
            "If any abilities trigger on the target creatures being destroyed, those abilities won't be put " +
                "onto the stack until after you've put creature cards onto the battlefield from graveyards."
        )
        ruling(
            "2018-07-13",
            "You may activate Liliana's last ability without choosing any target creatures. You'll just put " +
                "up to two creature cards onto the battlefield from graveyards. However, if you choose any " +
                "targets and all of those targets become illegal before the ability tries to resolve, the " +
                "ability won't resolve and you won't put any cards onto the battlefield."
        )
    }
}
