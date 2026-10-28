package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nahiri, Heir of the Ancients — Zendikar Rising #230 (canonical printing)
 * {2}{R}{W} · Legendary Planeswalker — Nahiri · Loyalty 4
 *
 * +1: Create a 1/1 white Kor Warrior creature token. You may attach an Equipment you control to it.
 * −2: Look at the top six cards of your library. You may reveal a Warrior or Equipment card from
 *     among them and put it into your hand. Put the rest on the bottom of your library in a random
 *     order.
 * −3: Nahiri deals damage to target creature or planeswalker equal to twice the number of
 *     Equipment you control.
 *
 * The +1 is Reckless Crew's attach pipeline cut down to one token: the token is created, then up to
 * one Equipment you control is chosen (the "you may") and attached to the token just made
 * ([CollectionSlot.CreatedTokens], as Animal Friend reads it) — all inside the one resolution, per
 * the 2020-09-25 ruling. An Equipment that can't legally equip the token is skipped by the attach
 * executor.
 *
 * The −2 is Tazri, Beacon of Unity's look-six-reveal-one shape with a Warrior-or-Equipment filter.
 *
 * The −3 counts Equipment you control on resolution (projected battlefield count) and doubles it,
 * as Practiced Tactics doubles its party size.
 */
val NahiriHeirOfTheAncients = card("Nahiri, Heir of the Ancients") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Nahiri"
    startingLoyalty = 4
    oracleText = "+1: Create a 1/1 white Kor Warrior creature token. You may attach an Equipment " +
        "you control to it.\n" +
        "−2: Look at the top six cards of your library. You may reveal a Warrior or Equipment card " +
        "from among them and put it into your hand. Put the rest on the bottom of your library in " +
        "a random order.\n" +
        "−3: Nahiri deals damage to target creature or planeswalker equal to twice the number of " +
        "Equipment you control."

    // +1: Create a 1/1 white Kor Warrior creature token. You may attach an Equipment you control to it.
    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            run(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Kor", "Warrior"),
                    imageUri = "https://cards.scryfall.io/normal/front/1/3/13412797-f831-49fe-adf3-369cdfacdbd5.jpg?1783929500",
                )
            )
            val equipment = gather(
                GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                player = Player.You,
            )
            val chosen = chooseUpTo(
                1,
                from = equipment,
                useTargetingUI = true,
                prompt = "You may choose an Equipment you control to attach to the Kor Warrior token",
            )
            run(
                Effects.ForEachInCollection(
                    chosen,
                    Effects.AttachTargetEquipmentToCreature(
                        equipmentTarget = EffectTarget.IterationEntity,
                        creatureTarget = CollectionSlot.CreatedTokens.asTarget,
                    )
                )
            )
        }
    }

    // −2: Look at the top six cards of your library. You may reveal a Warrior or Equipment card
    // from among them and put it into your hand. Put the rest on the bottom in a random order.
    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Any.withAnySubtype("Warrior", "Equipment"),
                prompt = "You may reveal a Warrior or Equipment card to put into your hand.",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
    }

    // −3: Nahiri deals damage to target creature or planeswalker equal to twice the number of
    // Equipment you control.
    loyaltyAbility(-3) {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withSubtype(Subtype.EQUIPMENT),
            ).count() * 2,
            victim,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "230"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/408be425-b8a6-4c03-b2a6-7ff7bd6555e0.jpg?1783929322"

        ruling("2020-09-25", "You create a Kor Warrior token and attach an Equipment to it if you wish all while Nahiri's first ability is resolving. Nothing can happen in between, and no player may choose to take actions.")
    }
}
