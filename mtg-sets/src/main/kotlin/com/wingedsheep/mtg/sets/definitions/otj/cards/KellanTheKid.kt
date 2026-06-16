package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kellan, the Kid
 * {G}{W}{U}
 * Legendary Creature — Human Faerie Rogue
 * 3/3
 *
 * Flying, lifelink
 * Whenever you cast a spell from anywhere other than your hand, you may cast a permanent spell
 * with equal or lesser mana value from your hand without paying its mana cost. If you don't,
 * you may put a land card from your hand onto the battlefield.
 *
 * The trigger uses the negated cast-source predicate [SpellCastPredicate.CastFromZoneOtherThan]
 * (Zone.HAND) — it fires only on casts from a known zone that is not the hand (Adventure/exile,
 * plotted cards, flashback, top of library, …).
 *
 * The payoff is an [Effects.IfYouDo]: the *action* gathers permanent cards from hand, filters
 * them to mana value ≤ the triggering spell's mana value
 * ([ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE]), lets you choose up to one, and casts it for
 * free (publishing the cast id to `kellanCast`). Success is gated on that collection being
 * non-empty; "if you don't" (no permanent cast) offers the optional "put a land from your hand
 * onto the battlefield" via the choose-up-to-one [Patterns.Hand.putFromHand].
 */
val KellanTheKid = card("Kellan, the Kid") {
    manaCost = "{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Human Faerie Rogue"
    power = 3
    toughness = 3
    oracleText = "Flying, lifelink\n" +
        "Whenever you cast a spell from anywhere other than your hand, you may cast a permanent " +
        "spell with equal or lesser mana value from your hand without paying its mana cost. If " +
        "you don't, you may put a land card from your hand onto the battlefield."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            requires = setOf(SpellCastPredicate.CastFromZoneOtherThan(Zone.HAND)),
        )
        effect = Effects.IfYouDo(
            action = Effects.Composite(
                GatherCardsEffect(
                    CardSource.FromZone(Zone.HAND, filter = GameObjectFilter.Permanent),
                    storeAs = "kellanCandidates",
                ),
                FilterCollectionEffect(
                    from = "kellanCandidates",
                    filter = CollectionFilter.ManaValueAtMost(
                        DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE),
                    ),
                    storeMatching = "kellanEligible",
                ),
                SelectFromCollectionEffect(
                    from = "kellanEligible",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "kellanChosen",
                    selectedLabel = "Cast without paying its mana cost",
                ),
                Effects.CastFromCollectionWithoutPayingCost("kellanChosen", storeCastTo = "kellanCast"),
            ),
            ifYouDo = Effects.Composite(emptyList()),
            ifYouDont = Patterns.Hand.putFromHand(GameObjectFilter.Land),
            successCriterion = SuccessCriterion.CollectionNonEmpty("kellanCast"),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "213"
        artist = "Magali Villeneuve"
        flavorText = "\"I found out who I was. Now it's time to discover who I am.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04dfbc4c-ab21-45db-bbd9-b9d245d60015.jpg?1712356134"

        ruling("2024-04-12", "Kellan's ability resolves before the spell that caused it to trigger.")
        ruling("2024-04-12", "If you cast a permanent spell this way, you don't get to put a land onto the battlefield, even if you couldn't cast a spell with equal or lesser mana value.")
    }
}
