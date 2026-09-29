package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.RepeatCondition
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Pipeline key for the card picked on one pass of chapter III's "any number of spells" loop. */
private const val GREAT_SYNTHESIS_PICK = "greatSynthesisPick"

/**
 * Jin-Gitaxias // The Great Synthesis — March of the Machine #65
 * {3}{U}{U} · Legendary Creature — Phyrexian Praetor 5/5 // Enchantment — Saga
 *
 * Chapter I's "you have no maximum hand size for as long as you control this Saga" is modelled as a
 * [NoMaximumHandSize] static on the Saga face: it lasts exactly while you control the Saga, and the
 * only window where the two readings differ (between the Saga entering and chapter I resolving) never
 * contains a cleanup step.
 *
 * Chapter III's "cast any number of spells from your hand without paying their mana costs" is a
 * do-while loop (the Cultivator Colossus idiom): each pass re-gathers the nonland cards in hand, lets
 * you pick up to one, and casts it for free through the synthesized-cast path (so card-type timing
 * is ignored, per the ruling); the loop continues while a card was picked this pass. Choosing none
 * ends it, and then the Saga exiles itself and returns as Jin-Gitaxias before any of those spells
 * resolve.
 */
private val TheGreatSynthesis = card("The Great Synthesis") {
    manaCost = ""
    colorIndicator = "U"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Draw cards equal to the number of cards in your hand. You have no maximum hand size for " +
        "as long as you control this Saga.\n" +
        "II — Return all non-Phyrexian creatures to their owners' hands.\n" +
        "III — You may cast any number of spells from your hand without paying their mana costs. " +
        "Exile this Saga, then return it to the battlefield (front face up)."

    staticAbility {
        ability = NoMaximumHandSize
    }

    // I — Draw cards equal to the number of cards in your hand (counted on resolution).
    sagaChapter(1) {
        effect = Effects.DrawCards(DynamicAmounts.cardsInYourHand())
    }

    // II — Return all non-Phyrexian creatures to their owners' hands.
    sagaChapter(2) {
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype.PHYREXIAN))
        )
    }

    // III — cast any number of spells from hand for free, then flip back to Jin-Gitaxias.
    sagaChapter(3) {
        effect = Effects.RepeatWhile(
            body = Effects.Pipeline {
                val spells = gather(CardSource.FromZone(Zone.HAND, filter = GameObjectFilter.Nonland))
                val pick = chooseUpTo(
                    1,
                    from = spells,
                    selectedLabel = "Cast without paying its mana cost",
                    name = GREAT_SYNTHESIS_PICK
                )
                run(Effects.CastFromCollectionWithoutPayingCost(pick))
            },
            repeatCondition = RepeatCondition.WhileCondition(
                Conditions.CollectionContainsMatch(GREAT_SYNTHESIS_PICK)
            )
        ) then Effects.ExileAndReturnTransformed(EffectTarget.Self, ReturnFace.FRONT)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "65"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/back/4/1/41c83142-b948-4ee5-a486-41306d2bb411.jpg?1783917044"
    }
}

private val JinGitaxiasFront = card("Jin-Gitaxias") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    power = 5
    toughness = 5
    oracleText = "Ward {2}\n" +
        "Whenever you cast a noncreature spell with mana value 3 or greater, draw a card.\n" +
        "{3}{U}: Exile Jin-Gitaxias, then return it to the battlefield transformed under its owner's " +
        "control. Activate only as a sorcery and only if you have seven or more cards in hand."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature.manaValueAtLeast(3))
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.CardsInHandAtLeast(7)))
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "65"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41c83142-b948-4ee5-a486-41306d2bb411.jpg?1783917044"
        ruling(
            "2023-04-14",
            "Once you've activated Jin-Gitaxias's last ability, it doesn't matter what happens to the " +
                "number of cards in your hand. The ability will resolve even if you have six or fewer " +
                "cards in your hand by that time."
        )
        ruling(
            "2023-04-14",
            "For chapter I of The Great Synthesis, the number of cards you draw is determined as the " +
                "ability resolves. If you no longer control The Great Synthesis at that time, your " +
                "maximum hand size remains unchanged."
        )
        ruling(
            "2023-04-14",
            "All the spells you cast due to chapter III are cast during the resolution of that ability. " +
                "You can cast them in any order, and timing restrictions based on the cards' types are " +
                "ignored. Once you're done casting spells, before any of them resolve, The Great " +
                "Synthesis will be exiled and return as Jin-Gitaxias."
        )
        ruling(
            "2023-04-14",
            "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it " +
                "without paying its mana cost."
        )
    }
}

val JinGitaxias: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = JinGitaxiasFront,
    backFace = TheGreatSynthesis,
)
