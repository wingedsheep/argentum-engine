package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player

/**
 * In Search of Greatness
 * {G}{G}
 * Enchantment
 * At the beginning of your upkeep, you may cast a permanent spell from your hand with mana value
 * equal to 1 plus the greatest mana value among other permanents you control without paying its
 * mana cost. If you don't, scry 1.
 *
 * The Baral and Kari Zev shape: gather the hand, keep the nonland permanent cards whose mana value
 * equals 1 + the greatest mana value among the *other* permanents you control (read as the trigger
 * resolves, per the ruling; `excludeSelf` drops In Search of Greatness itself, and an empty or
 * lands-only board aggregates to 0, so MV 1 is castable), choose up to one, cast it for free.
 * "If you don't" — nothing was actually cast — scries 1.
 */
val InSearchOfGreatness = card("In Search of Greatness") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may cast a permanent spell from your hand with " +
        "mana value equal to 1 plus the greatest mana value among other permanents you control " +
        "without paying its mana cost. If you don't, scry 1."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.IfYouDo(
            action = Effects.Pipeline {
                val inHand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val eligible = filter(
                    inHand,
                    GameObjectFilter.NonlandPermanent.manaValueEqualsDynamic(
                        DynamicAmounts.battlefield(Player.You, excludeSelf = true).maxManaValue() + 1
                    )
                )
                val chosen = chooseUpTo(
                    1,
                    from = eligible,
                    selectedLabel = "Cast without paying its mana cost"
                )
                run(Effects.CastFromCollectionWithoutPayingCost(chosen, storeCastTo = "greatnessCast"))
            },
            then = Effects.Nothing,
            otherwise = Effects.Scry(1),
            successCriterion = SuccessCriterion.CollectionNonEmpty("greatnessCast"),
        )
        description = "At the beginning of your upkeep, you may cast a permanent spell from your hand with " +
            "mana value equal to 1 plus the greatest mana value among other permanents you control " +
            "without paying its mana cost. If you don't, scry 1."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac0e50fa-5114-4f96-89f4-000906da7c76.jpg?1783928212"

        ruling(
            "2021-02-05",
            "A permanent spell is an artifact spell, a creature spell, an enchantment spell, or a planeswalker spell."
        )
        ruling(
            "2021-02-05",
            "Consider the other permanents you control as the triggered ability resolves to determine which " +
                "spell you can cast. If the only other permanents you control are lands, or in the unlikely " +
                "event you don't control any other permanents, you may cast a permanent spell with mana value " +
                "1 from your hand."
        )
        ruling(
            "2021-02-05",
            "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any " +
                "alternative costs. You can, however, pay additional costs, such as kicker costs. If the card " +
                "has any mandatory additional costs, those must be paid to cast the spell."
        )
        ruling("2021-02-05", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2021-02-05", "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X.")
    }
}
