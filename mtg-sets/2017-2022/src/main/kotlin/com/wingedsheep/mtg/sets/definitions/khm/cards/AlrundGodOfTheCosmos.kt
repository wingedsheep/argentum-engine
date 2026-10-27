package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ofChosenCardTypeFromVariable
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val AlrundGodOfTheCosmosFront = card("Alrund, God of the Cosmos") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — God"
    power = 1
    toughness = 1
    oracleText = "Alrund gets +1/+1 for each card in your hand and each foretold card you own in exile.\n" +
        "At the beginning of your end step, choose a card type, then reveal the top two cards of your library. " +
        "Put all cards of the chosen type revealed this way into your hand and the rest on the bottom of your " +
        "library in any order."

    val bonus = DynamicAmounts.cardsInYourHand() +
        DynamicAmounts.count(Player.You, Zone.EXILE, GameObjectFilter.Any.foretold())
    staticAbility {
        ability = GrantDynamicStats(filter = GroupFilter.source(), powerBonus = bonus, toughnessBonus = bonus)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val type = chooseOption(OptionType.CARD_TYPE)
            run(
                Patterns.Library.revealTopPutAllMatchingToHand(
                    count = 2,
                    filter = GameObjectFilter.Any.ofChosenCardTypeFromVariable(type),
                    restOrder = CardOrder.ControllerChooses,
                )
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "40"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d131784-c1a3-463e-a37b-b720af67ab62.jpg?1783928276"
        ruling("2021-02-05", "Alrund's first ability applies only while Alrund is on the battlefield. In every other zone, Alrund is 1/1.")
        ruling("2021-02-05", "You may choose any card type, but the ones that may appear in your library are artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, and sorcery. Supertypes such as snow and basic may not be chosen. Subtypes such as God, Aura, and Forest also may not be chosen.")
    }
}

private val HakkaWhisperingRavenBack = card("Hakka, Whispering Raven") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Bird"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever Hakka deals combat damage to a player, return it to its owner's hand, then scry 2."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ReturnToHand(EffectTarget.Self) then Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "40"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/back/5/d/5d131784-c1a3-463e-a37b-b720af67ab62.jpg?1783928276"
    }
}

val AlrundGodOfTheCosmos: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = AlrundGodOfTheCosmosFront,
    backFace = HakkaWhisperingRavenBack,
)
