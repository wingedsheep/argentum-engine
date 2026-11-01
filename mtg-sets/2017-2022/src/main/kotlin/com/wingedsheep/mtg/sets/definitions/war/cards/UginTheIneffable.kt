package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ugin, the Ineffable — War of the Spark #2 (canonical printing)
 * {6} · Legendary Planeswalker — Ugin · Loyalty 4
 *
 * Colorless spells you cast cost {2} less to cast.
 * +1: Exile the top card of your library face down and look at it. Create a 2/2 colorless Spirit
 * creature token. When that token leaves the battlefield, put the exiled card into your hand.
 * −3: Destroy target permanent that's one or more colors.
 *
 * The +1 exiles the card face down but lookable by its owner (The Dragon-Kami Reborn's shape),
 * then sets up one delayed "leaves the battlefield" trigger per token it created (Stangg's
 * watched-token shape), each carrying the exiled card. The trigger is Ugin's, not the token's, so
 * it still fires after Ugin has left (ruling) and can't be stripped from the token. Because the
 * carried card keeps its object identity, the first trigger to resolve moves it to hand and any
 * later one — a second token from a doubler (ruling) — finds it already gone and does nothing.
 */
val UginTheIneffable = card("Ugin, the Ineffable") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Legendary Planeswalker — Ugin"
    startingLoyalty = 4
    oracleText = "Colorless spells you cast cost {2} less to cast.\n" +
        "+1: Exile the top card of your library face down and look at it. Create a 2/2 colorless " +
        "Spirit creature token. When that token leaves the battlefield, put the exiled card into your hand.\n" +
        "−3: Destroy target permanent that's one or more colors."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless)),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1))
            move(
                exiled,
                CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN,
                lookableInExile = true,
            )
            run(
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = emptySet(),
                    creatureTypes = setOf("Spirit"),
                    imageUri = "https://cards.scryfall.io/normal/front/2/4/243d7514-35a9-446b-be0d-a5b6b4001291.jpg?1783933354",
                )
            )
            run(
                Effects.ForEachInCollection(
                    CollectionSlot.CreatedTokens,
                    Effects.CreateDelayedTrigger(
                        trigger = Triggers.self.leaves(),
                        watchedTarget = EffectTarget.IterationEntity,
                        effect = Effects.Pipeline { toHand(exiled) },
                        expiry = DelayedTriggerExpiry.Never,
                        fireOnce = true,
                        carryCollections = listOf(exiled.key),
                    )
                )
            )
        }
        description = "Exile the top card of your library face down and look at it. Create a 2/2 " +
            "colorless Spirit creature token. When that token leaves the battlefield, put the exiled " +
            "card into your hand."
    }

    loyaltyAbility(-3) {
        val colored = target(
            TargetFilter(GameObjectFilter.Permanent.withCardPredicate(CardPredicate.IsColored))
        )
        effect = Effects.Destroy(colored)
        description = "Destroy target permanent that's one or more colors."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b003521-3da3-41bf-9765-36630653f902.jpg?1783933487"
        ruling("2019-05-03", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Ugin). The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2019-05-03", "A colorless spell whose mana cost is {2} or {1} will cost {0} to cast.")
        ruling("2019-05-03", "Once you look at the exiled face-down card once, you may look at it again any time you wish. If another player gains control of the Spirit token, that player can't look at the exiled card.")
        ruling("2019-05-03", "If the Spirit leaves the battlefield for any reason, you'll put the exiled card into your hand, even if Ugin left the battlefield before that happened.")
        ruling("2019-05-03", "If an effect such as that of Doubling Season causes Ugin to create two Spirit tokens, you still only exile one card from your library. You'll put that card into your hand the first time that either of the tokens leaves the battlefield.")
        ruling("2019-05-03", "A land normally has no color, even if it can produce one or more colors of mana.")
    }
}
